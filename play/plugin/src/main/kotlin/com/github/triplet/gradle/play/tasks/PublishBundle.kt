package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.releaseStatusOrDefault
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.github.triplet.gradle.play.internal.trackOrDefault
import com.github.triplet.gradle.play.internal.userFractionOrDefault
import com.github.triplet.gradle.play.tasks.internal.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.findBundleFile
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
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
) : PublishArtifactTaskBase(extension, variant), PublishableTrackExtensionOptions {
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
            edits2.uploadBundle(
                    parameters.bundleFile.get().asFile,
                    parameters.mappingFile.orNull?.asFile,
                    config.resolutionStrategyOrDefault,
                    parameters.versionCode.get().toLong(),
                    parameters.variantName.get(),
                    parameters.skippedMarker.get().asFile.exists(),
                    config.releaseStatusOrDefault,
                    config.trackOrDefault,
                    config.retain.artifacts,
                    findReleaseName(),
                    findReleaseNotes(),
                    config.userFractionOrDefault
            )
        }

        interface Params : ArtifactPublishingParams {
            val bundleFile: RegularFileProperty
        }
    }
}
