package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.PlayPublishPackageBase
import com.github.triplet.gradle.play.internal.TrackType
import com.google.api.services.androidpublisher.AndroidPublisher
import org.gradle.api.tasks.TaskAction

open class ModifyReleaseTask : PlayPublishPackageBase() {
    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun modify() = write { editId ->
        progressLogger.start("Modifies release tracks for variant ${variant.name}", null)

        requireNotNull(extension._fromTrack) {
            progressLogger.progress("No from-track provided to modify", true)
        }
        val startCodes = downloadVersionCodes(editId, extension._fromTrack!!)
        if (startCodes == null || startCodes.isEmpty()) {
            progressLogger.progress("No versions published on track '${extension.track}' for ${variant.name}", true)
        } else {
            updateTracks(editId, startCodes)
        }
        progressLogger.completed()
    }

    private fun AndroidPublisher.Edits.downloadVersionCodes(editId: String, track: TrackType): List<Long>? {
        progressLogger.progress("Downloading active version codes")
        return tracks().list(variant.applicationId, editId).execute().tracks
                ?.filter { it.track == track.publishedName }
                ?.map { it.releases ?: emptyList() }?.flatten()
                ?.map { it.versionCodes ?: emptyList() }?.flatten()
    }
}
