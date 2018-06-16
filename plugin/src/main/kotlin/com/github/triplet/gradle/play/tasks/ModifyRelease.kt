package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.internal.PlayPublishPackageBase
import com.google.api.services.androidpublisher.AndroidPublisher
import org.gradle.api.tasks.TaskAction

open class ModifyRelease : PlayPublishPackageBase() {
    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun modify() = write { editId ->
        progressLogger.start("Modifies release tracks for variant ${variant.name}", null)

        val startCodes = downloadVersionCodes(editId)
        if (startCodes.isEmpty()) {
            logger.warn("No versions published on track '${extension.track}' for ${variant.name}")
        } else {
            updateTracks(editId, startCodes)
        }

        progressLogger.completed()
    }

    private fun AndroidPublisher.Edits.downloadVersionCodes(editId: String): List<Long> {
        progressLogger.progress("Downloading active version codes")

        val fromTrack = requireNotNull(extension._fromTrack) { "A 'fromTrack' must be specified" }
        return tracks().list(variant.applicationId, editId).execute().tracks
                ?.filter { it.track == fromTrack.publishedName }
                ?.map { it.releases ?: emptyList() }?.flatten()
                ?.map { it.versionCodes ?: emptyList() }?.flatten()
                .orEmpty()
    }
}
