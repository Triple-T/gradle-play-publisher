package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.MIME_TYPE_STREAM
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.tasks.internal.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PlayPublishArtifactBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.TransientTrackOptions
import com.github.triplet.gradle.play.tasks.internal.findBundleFile
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

abstract class PublishBundle @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        variant: ApplicationVariant,
        optionsHolder: TransientTrackOptions.Holder
) : PlayPublishArtifactBase(extension, variant, optionsHolder), PublishableTrackExtensionOptions {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    val bundle
        get() = findBundleFile()
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:OutputDirectory // This directory isn't used, but it's needed for up-to-date checks to work
    protected val outputDir by lazy { File(project.buildDir, "${variant.playPath}/bundles") }

    @TaskAction
    fun publishBundle() {
        val bundleFile = bundle?.orNull() ?: return
        project.serviceOf<WorkerExecutor>().submit(BundleUploader::class) {
            paramsForBase(this, BundleUploader.Params(bundleFile))
        }
    }

    private class BundleUploader @Inject constructor(
            private val p: Params,
            data: ArtifactPublishingParams
    ) : ArtifactWorkerBase(data) {
        override fun upload() {
            val content = FileContent(MIME_TYPE_STREAM, p.bundleFile)
            val bundle = try {
                edits.bundles().upload(appId, editId, content)
                        .trackUploadProgress("App Bundle", p.bundleFile)
                        .execute()
            } catch (e: GoogleJsonResponseException) {
                handleUploadFailures(e, content.file)
            } ?: return

            handleArtifactDetails(editId, bundle.versionCode)
            updateTracks(editId, listOf(bundle.versionCode.toLong()))
        }

        data class Params(val bundleFile: File) : Serializable
    }
}
