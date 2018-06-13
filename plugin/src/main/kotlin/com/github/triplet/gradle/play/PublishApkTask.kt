package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.PlayPublishPackageBase
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.trackUploadProgress
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

open class PublishApkTask : PlayPublishPackageBase() {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val inputApks by lazy {
        // TODO: If we take a customizable folder, we can fix #233, #227
        variant.outputs.filterIsInstance<ApkVariantOutput>().map { it.outputFile }
    }
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory
    val outputDir by lazy { File(project.buildDir, "${variant.playPath}/apks") }

    @TaskAction
    fun publishApks(inputs: IncrementalTaskInputs) = write { editId: String ->
        progressLogger.start("Uploads APK files for variant ${variant.name}", null)

        if (!inputs.isIncremental) project.delete(outputs.files)

        val publishedApks = mutableListOf<Apk>()
        inputs.outOfDate {
            val file = it.file
            if (inputApks.contains(file)) {
                project.copy {
                    it.from(file)
                    it.into(outputDir)
                }

                publishApk(editId, FileContent(MIME_TYPE_APK, file))?.let { publishedApks += it }
            }
        }
        inputs.removed { project.delete(File(outputDir, it.file.name)) }

        if (publishedApks.isNotEmpty()) {
            updateTracks(editId, publishedApks.map { it.versionCode.toLong() })
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

    private companion object {
        const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        const val MIME_TYPE_STREAM = "application/octet-stream"
    }
}
