package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PlayPublishArtifactBase
import com.github.triplet.gradle.play.tasks.internal.UpdatableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.Serializable
import javax.inject.Inject

open class PromoteRelease @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PlayPublishArtifactBase(extension, variant), UpdatableTrackExtensionOptions {
    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun promote() {
        project.serviceOf<WorkerExecutor>().submit(Promoter::class) {
            paramsForBase(this, Promoter.Params())
        }
    }

    private class Promoter @Inject constructor(
            @Suppress("UNUSED_PARAMETER") p: Params,
            artifact: ArtifactPublishingData,
            play: PlayPublishingData
    ) : ArtifactWorkerBase(artifact, play) {
        override fun upload() {
            val tracks = edits.tracks().list(appId, editId).execute()
                    .tracks.orEmpty()
                    .filter {
                        it.releases.orEmpty().flatMap { it.versionCodes.orEmpty() }.isNotEmpty()
                    }

            if (tracks.isEmpty()) {
                logger.warn("Nothing to promote. Did you mean to run publish?")
                return
            }

            val track = run {
                val from = extension._fromTrack
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
                        updateStatus = extension._releaseStatus != null,
                        updateFraction = extension._userFraction != null,
                        updateConsoleName = false
                )
            }

            // Duplicate statuses are not allowed, so only keep the unique ones from the highest
            // version code.
            track.releases = track.releases.sortedByDescending {
                it.versionCodes?.max()
            }.distinctBy {
                it.status
            }

            println("Promoting ${track.releases.map { it.status }.distinct()} release " +
                            "($appId:${track.releases.flatMap { it.versionCodes.orEmpty() }}) " +
                            "from track '${track.track}' to track '${extension.track}'")
            edits.tracks().update(appId, editId, extension.track, track).execute()
        }

        class Params : Serializable
    }
}
