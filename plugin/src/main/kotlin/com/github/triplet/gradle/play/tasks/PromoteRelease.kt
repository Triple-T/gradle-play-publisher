package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.tasks.internal.PlayPublishPackageBase
import org.gradle.api.tasks.TaskAction

open class PromoteRelease : PlayPublishPackageBase() {
    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun promote() = write { editId: String ->
        progressLogger.start("Promotes artifacts for variant ${variant.name}", null)

        val tracks = tracks().list(variant.applicationId, editId).execute().tracks.orEmpty()

        if (tracks.isEmpty()) {
            logger.warn("Nothing to promote. Did you mean to run publish?")
            return@write
        }

        val track = run {
            val from = extension._fromTrack
            if (from == null) {
                tracks.last() // Tracks are always ordered from most to least stable
            } else {
                val name = from.publishedName
                checkNotNull(tracks.find { it.track.equals(name, true) }) {
                    "${name.capitalize()} track has no active artifacts"
                }
            }
        }
        check(track.releases.orEmpty().map { it.versionCodes.orEmpty() }.flatten().isNotEmpty()) {
            "${track.track.capitalize()} track has no releases"
        }
        progressLogger.progress("Promoting '${track.track}' release to '${extension.track}'")

        track.releases.forEach {
            it.applyChanges(
                    updateStatus = extension._releaseStatus != null,
                    useDefaultReleaseNotes = false,
                    updateFraction = extension._userFraction != null
            )
        }
        tracks().update(variant.applicationId, editId, extension.track, track).execute()

        progressLogger.completed()
    }
}
