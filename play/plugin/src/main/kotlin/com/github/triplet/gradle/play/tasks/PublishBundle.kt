package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.releaseStatusOrDefault
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.github.triplet.gradle.play.internal.trackOrDefault
import com.github.triplet.gradle.play.internal.userFractionOrDefault
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.UploadArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.findBundleFile
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
        variant: ApplicationVariant
) : UploadArtifactTaskBase(extension, variant), PublishableTrackExtensionOptions {
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

    abstract class BundleUploader : UploadArtifactWorkerBase<BundleUploader.Params>() {
        override fun upload() {
            val bundleFile = parameters.bundleFile.get().asFile
            edits.uploadBundle(
                    bundleFile,
                    parameters.mappingFile.orNull?.asFile,
                    config.resolutionStrategyOrDefault,
                    findBestVersionCode(bundleFile),
                    parameters.variantName.get(),
                    parameters.skippedMarker.get().asFile.exists(),
                    config.trackOrDefault,
                    config.releaseStatusOrDefault,
                    findReleaseName(config.trackOrDefault),
                    findReleaseNotes(config.trackOrDefault),
                    config.userFractionOrDefault,
                    config.retain.artifacts
            )
        }

        interface Params : ArtifactUploadingParams {
            val bundleFile: RegularFileProperty
        }
    }
}
