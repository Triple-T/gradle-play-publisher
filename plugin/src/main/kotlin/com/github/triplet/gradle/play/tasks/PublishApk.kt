package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.EXPANSION_FILES_PATH
import com.github.triplet.gradle.play.internal.PlayPublishPackageBase
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.expansionFileTypes
import com.github.triplet.gradle.play.internal.isDirectChildOf
import com.github.triplet.gradle.play.internal.nullOrFull
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.safeCreateNewFile
import com.github.triplet.gradle.play.internal.trackUploadProgress
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ExpansionFile
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

open class PublishApk : PlayPublishPackageBase() {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val inputApks by lazy {
        // TODO: If we take a customizable folder, we can fix #233, #227
        variant.outputs.filterIsInstance<ApkVariantOutput>().map { it.outputFile }
    }
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputDirectory
    internal val expansionFilesDir by lazy { File(resDir, EXPANSION_FILES_PATH) }

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory
    internal val outputDir by lazy { File(project.buildDir, "${variant.playPath}/apks") }

    @TaskAction
    fun publishApks(inputs: IncrementalTaskInputs) = write { editId: String ->
        progressLogger.start("Uploads APK files for variant ${variant.name}", null)

        if (!inputs.isIncremental) project.delete(outputs.files)

        val publishedApks = mutableListOf<Apk>()
        val changedExpansionFiles = mutableListOf<File>()
        inputs.outOfDate {
            val file = it.file

            if (inputApks.contains(file)) {
                project.copy { it.from(file).into(outputDir) }
                publishApk(editId, FileContent(MIME_TYPE_APK, file))?.let { publishedApks += it }
            }

            if (file.isDirectChildOf(EXPANSION_FILES_PATH)) changedExpansionFiles += file
        }
        inputs.removed {
            val file = it.file

            project.delete(File(outputDir, file.name))
            if (file.isDirectChildOf(EXPANSION_FILES_PATH)) {
                project.delete(getOobCodeFile(file.nameWithoutExtension))
            }
        }

        if (publishedApks.isNotEmpty()) {
            val versionCodes = publishedApks.map { it.versionCode }
            updateTracks(editId, versionCodes.map(Int::toLong))
            uploadExpansionFiles(editId, versionCodes, changedExpansionFiles)
        } else if (changedExpansionFiles.isNotEmpty()) {
            logger.warn("New expansion files cannot be uploaded without a new APK")
        }

        progressLogger.completed()
    }

    private fun AndroidPublisher.Edits.publishApk(editId: String, apkFile: FileContent): Apk? {
        val apk = try {
            apks().upload(variant.applicationId, editId, apkFile)
                    .trackUploadProgress(progressLogger, "APK")
                    .execute()
        } catch (e: GoogleJsonResponseException) {
            val isConflict = e.details.errors.all {
                it.reason == "apkUpgradeVersionConflict" || it.reason == "apkNoUpgradePath"
            }
            if (isConflict) {
                when (extension._resolutionStrategy) {
                    ResolutionStrategy.AUTO -> throw IllegalStateException(
                            "Concurrent uploads for variant ${variant.name}. Make sure to " +
                                    "synchronously upload your APKs such that they don't conflict.",
                            e
                    )
                    ResolutionStrategy.FAIL -> throw IllegalStateException(
                            "Version code ${variant.versionCode} is too low for variant ${variant.name}.",
                            e
                    )
                    ResolutionStrategy.IGNORE -> logger.warn(
                            "Ignoring APK ($apkFile) for version code ${variant.versionCode}")
                }
                return null
            } else {
                throw e
            }
        }

        if (variant.mappingFile?.exists() == true) {
            val content = FileContent(MIME_TYPE_STREAM, variant.mappingFile)
            deobfuscationfiles()
                    .upload(variant.applicationId, editId, apk.versionCode, "proguard", content)
                    .trackUploadProgress(progressLogger, "mapping file")
                    .execute()
        }

        return apk
    }

    private fun AndroidPublisher.Edits.uploadExpansionFiles(
            editId: String,
            versionCodes: List<Int>,
            changedExpansionFiles: List<File>
    ) {
        val savedCodeFiles = expansionFileTypes.map { getOobCodeFile(it) }

        progressLogger.progress("Linking expansion files to new APKs")
        for (file in savedCodeFiles) {
            val savedCode = file.orNull()?.readText().nullOrFull()?.toInt() ?: continue
            for (newCode in versionCodes) {
                expansionfiles().update(
                        variant.applicationId,
                        editId,
                        newCode,
                        file.extension,
                        ExpansionFile().apply { referencesVersion = savedCode }
                ).execute()
            }
        }

        val minCode = versionCodes.min() ?: 1
        for (file in savedCodeFiles) {
            if (changedExpansionFiles.map { it.nameWithoutExtension }.contains(file.extension)) {
                file.safeCreateNewFile().writeText(minCode.toString())
            }
        }

        for (file in changedExpansionFiles) {
            val type = file.nameWithoutExtension
            expansionfiles().upload(
                    variant.applicationId,
                    editId,
                    minCode,
                    type,
                    FileContent(MIME_TYPE_STREAM, file)
            ).trackUploadProgress(progressLogger, "$type expansion file").execute()

            progressLogger.progress("Adding expansion file '$type' to other APKs")
            for (newCode in versionCodes) {
                if (newCode == minCode) continue // The upload automatically updates it for us
                expansionfiles().update(
                        variant.applicationId,
                        editId,
                        newCode,
                        type,
                        ExpansionFile().apply { referencesVersion = minCode }
                ).execute()
            }
        }
    }

    private fun getOobCodeFile(name: String) = File(outputDir, "oob-code-${variant.baseName}.$name")

    private companion object {
        const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        const val MIME_TYPE_STREAM = "application/octet-stream"
    }
}
