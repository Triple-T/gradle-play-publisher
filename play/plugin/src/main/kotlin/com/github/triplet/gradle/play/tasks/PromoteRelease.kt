package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.promoteTrackOrDefault
import com.github.triplet.gradle.play.tasks.internal.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.UpdatableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import org.gradle.api.logging.Logging
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
            val tracks = edits.tracks().list(appId, editId).execute()
                    .tracks.orEmpty()
                    .filter {
                        it.releases.orEmpty().flatMap { it.versionCodes.orEmpty() }.isNotEmpty()
                    }

            if (tracks.isEmpty()) {
                Logging.getLogger(PromoteRelease::class.java)
                        .warn("Nothing to promote. Did you mean to run publish?")
                return
            }

            val track = run {
                val from = config.fromTrack
                if (from == null) {
                    tracks.sortedByDescending {
                        it.releases.flatMap { it.versionCodes.orEmpty() }.max()
                    }.first()
                } else {
                    checkNotNull(tracks.find { it.track.equals(from, true) }) {
                        "${from.capitalize()} track has no active artifacts"
                    }
                }
            }

            track.releases.forEach {
                it.applyChanges(
                        updateStatus = config.releaseStatus != null,
                        updateFraction = config.userFraction != null,
                        updateConsoleName = config.releaseName != null
                )
            }

            // Duplicate statuses are not allowed, so only keep the unique ones from the highest
            // version code.
            track.releases = track.releases.sortedByDescending {
                it.versionCodes?.max()
            }.distinctBy {
                it.status
            }

            val promoteTrackName = config.promoteTrackOrDefault
            println("Promoting ${track.releases.map { it.status }.distinct()} release " +
                            "($appId:${track.releases.flatMap { it.versionCodes.orEmpty() }}) " +
                            "from track '${track.track}' to track '$promoteTrackName'")
            edits.tracks().update(appId, editId, promoteTrackName, track).execute()
        }
    }
}
