package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.internal.MIME_TYPE_STREAM
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.trackUploadProgress
import com.github.triplet.gradle.play.tasks.internal.PlayPublishPackageBase
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Bundle
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

open class PublishBundle : PlayPublishPackageBase() {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    val bundle by lazy {
        // TODO: If we take a customizable folder, we can fix #233, #227
        val archivesBaseName = project.properties["archivesBaseName"] as String
        File(project.buildDir, "outputs/bundle/${variant.name}/${archivesBaseName}.aab")
    }
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory
    val outputDir by lazy { File(project.buildDir, "${variant.playPath}/bundles") }

    @TaskAction
    fun publishBundle(inputs: IncrementalTaskInputs) = write { editId: String ->
        progressLogger.start("Uploads App Bundle for variant ${variant.name}", null)

        if (!inputs.isIncremental) project.delete(outputs.files)

        inputs.outOfDate {
            if (file == bundle) {
                project.copy { from(file).into(outputDir) }

                publishBundle(editId, FileContent(MIME_TYPE_STREAM, file))?.let {
                    updateTracks(editId, listOf(it.versionCode.toLong()))
                }
            }
        }
        inputs.removed { project.delete(File(outputDir, file.name)) }

        progressLogger.completed()
    }

    private fun AndroidPublisher.Edits.publishBundle(
            editId: String,
            content: FileContent
    ): Bundle? {
        val bundle = try {
            bundles().upload(variant.applicationId, editId, content)
                    .trackUploadProgress(progressLogger, "App Bundle")
                    .execute()
        } catch (e: GoogleJsonResponseException) {
            return e.handleUploadFailures(content.file)
        }

        handlePackageDetails(editId, bundle.versionCode)

        return bundle
    }
}
