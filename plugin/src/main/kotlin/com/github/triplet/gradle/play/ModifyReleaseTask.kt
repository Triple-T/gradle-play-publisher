package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.PlayPublishPackageBase
import com.github.triplet.gradle.play.internal.TrackType
import org.gradle.api.tasks.TaskAction

open class ModifyReleaseTask : PlayPublishPackageBase() {
    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    private var startCodes: List<Long>? = null

    @TaskAction
    fun modify() {
        progressLogger.start("Modifies release tracks for variant ${variant.name}", null)

        requireNotNull(extension._fromTrack) {
            progressLogger.completed("No from-track provided to modify", true)
        }
        downloadVersionCodes(extension._fromTrack!!)

        startCodes.let {
            if (it == null || it.isEmpty()) {
                progressLogger.completed("No versions published on track '${extension.track}' for ${variant.name}", false)
            } else {
                write { editId: String ->
                    updateTracks(editId, it)
                    progressLogger.completed()
                }
            }
        }
    }

    private fun downloadVersionCodes(track: TrackType) = read { editId ->
        progressLogger.progress("Downloading active version codes")
        startCodes = tracks().list(variant.applicationId, editId).execute().tracks
                ?.filter { it.track == track.publishedName }
                ?.map { it.releases ?: emptyList() }?.flatten()
                ?.map { it.versionCodes ?: emptyList() }?.flatten()
    }
}
