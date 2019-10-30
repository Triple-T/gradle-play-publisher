package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.promoteTrackOrDefault
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.UpdatableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.workers.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

internal abstract class PromoteRelease @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishArtifactTaskBase(extension, variant), UpdatableTrackExtensionOptions {
    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun promote() {
        project.serviceOf<WorkerExecutor>().noIsolation().submit(Promoter::class) {
            paramsForBase(this)
        }
    }

    abstract class Promoter :
            ArtifactWorkerBase<ArtifactWorkerBase.ArtifactPublishingParams>() {
        override fun upload() {
            edits2.promoteRelease(
                    config.promoteTrackOrDefault,
                    config.fromTrack,
                    config.releaseStatus,
                    findReleaseName(),
                    findReleaseNotes(),
                    config.userFraction,
                    config.retain.artifacts
            )
        }
    }
}
