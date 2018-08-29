package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.PlayPublishPackageBase
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
    @get:OutputDirectory
    internal val outputDir by lazy { File(project.buildDir, "${variant.playPath}/apks") }

    @TaskAction
    fun publishApks(inputs: IncrementalTaskInputs) = write { editId: String ->
        progressLogger.start("Uploads APK files for variant ${variant.name}", null)

        if (!inputs.isIncremental) project.delete(outputs.files)

        val publishedApks = mutableListOf<Apk>()
        inputs.outOfDate {
            if (inputApks.contains(file)) {
                project.copy { from(file).into(outputDir) }
                publishApk(editId, FileContent(MIME_TYPE_APK, file))?.let { publishedApks += it }
            }
        }
        inputs.removed { project.delete(File(outputDir, file.name)) }

        if (publishedApks.isNotEmpty()) {
            updateTracks(editId, publishedApks.map { it.versionCode.toLong() })
        }

        progressLogger.completed()
    }

    private fun AndroidPublisher.Edits.publishApk(editId: String, content: FileContent): Apk? {
        val apk = try {
            apks().upload(variant.applicationId, editId, content)
                    .trackUploadProgress(progressLogger, "APK")
                    .execute()
        } catch (e: GoogleJsonResponseException) {
            return e.handleUploadFailures(content.file)
        }

        handlePackageDetails(editId, apk.versionCode)

        return apk
    }

    private companion object {
        const val MIME_TYPE_APK = "application/vnd.android.package-archive"
    }
}
