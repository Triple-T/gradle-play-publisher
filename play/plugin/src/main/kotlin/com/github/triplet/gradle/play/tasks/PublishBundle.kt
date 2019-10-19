package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.MIME_TYPE_STREAM
import com.github.triplet.gradle.play.tasks.internal.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.TransientTrackOptions
import com.github.triplet.gradle.play.tasks.internal.findBundleFile
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

internal abstract class PublishBundle @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant,
        optionsHolder: TransientTrackOptions.Holder
) : PublishArtifactTaskBase(extension, variant, optionsHolder), PublishableTrackExtensionOptions {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    protected val bundle
        get() = findBundleFile()

    // This directory isn't used, but it's needed for up-to-date checks to work
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:Optional
    @get:OutputDirectory
    protected val outputDir = null

    @TaskAction
    fun publishBundle() {
        val bundle = bundle?.orNull() ?: return
        project.serviceOf<WorkerExecutor>().noIsolation().submit(BundleUploader::class) {
            paramsForBase(this)
            bundleFile.set(bundle)
        }
    }

    abstract class BundleUploader : ArtifactWorkerBase<BundleUploader.Params>() {
        override fun upload() {
            val bundleFile = parameters.bundleFile.get().asFile
            val bundle = try {
                edits.bundles().upload(appId, editId, FileContent(MIME_TYPE_STREAM, bundleFile))
                        .trackUploadProgress("App Bundle", bundleFile)
                        .execute()
            } catch (e: GoogleJsonResponseException) {
                handleUploadFailures(e, bundleFile)
            } ?: return

            uploadMappingFile(bundle.versionCode)
            updateTracks(listOf(bundle.versionCode.toLong()))
        }

        interface Params : ArtifactPublishingParams {
            val bundleFile: RegularFileProperty
        }
    }
}
