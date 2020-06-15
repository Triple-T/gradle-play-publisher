package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.UploadArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.UploadArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
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
        appId: String
) : UploadArtifactTaskBase(extension, appId), PublishableTrackExtensionOptions {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    internal abstract val bundle: RegularFileProperty

    // This directory isn't used, but it's needed for up-to-date checks to work
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:Optional
    @get:OutputDirectory
    protected val outputDir = null

    @TaskAction
    fun publishBundle() {
        project.serviceOf<WorkerExecutor>().noIsolation().submit(BundleUploader::class) {
            paramsForBase(this)
            bundleFile.set(bundle)
        }
    }

    abstract class BundleUploader : UploadArtifactWorkerBase<BundleUploader.Params>() {
        override fun upload() {
            val bundleFile = parameters.bundleFile.get().asFile
            edits.uploadBundle(
                    bundleFile,
                    parameters.mappingFile.orNull?.asFile,
                    config.resolutionStrategy,
                    parameters.skippedMarker.get().asFile.exists(),
                    config.track,
                    config.releaseStatus,
                    findReleaseName(config.track),
                    findReleaseNotes(config.track),
                    config.userFraction,
                    config.updatePriority,
                    config.retainArtifacts
            )
        }

        interface Params : ArtifactUploadingParams {
            val bundleFile: RegularFileProperty
        }
    }
}
