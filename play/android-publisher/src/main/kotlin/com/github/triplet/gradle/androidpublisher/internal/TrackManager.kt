package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease

internal interface TrackManager {
    data class UpdateConfig(
            val trackName: String,
            val versionCodes: List<Long>,
            val releaseStatus: ReleaseStatus,
            val userFraction: Double,
            val releaseNotes: Map<String, String?>,
            val retainableArtifacts: List<Long>?,
            val releaseName: String?,
            val isBuildSkippingCommit: Boolean
    )

    fun update(config: UpdateConfig)
}

internal class DefaultTrackManager(
        private val publisher: InternalPlayPublisher,
        private val editId: String
) : TrackManager {
    override fun update(config: TrackManager.UpdateConfig) {
        val track = if (config.isBuildSkippingCommit) {
            createTrackForSkippedCommit(config)
        } else if (config.releaseStatus.isRollout()) {
            createTrackForRollout(config)
        } else {
            createDefaultTrack(config)
        }

        publisher.updateTrack(editId, track)
    }

    private fun createTrackForSkippedCommit(config: TrackManager.UpdateConfig): Track {
        val track = publisher.getTrack(editId, config.trackName)

        track.releases = if (track.releases.isNullOrEmpty()) {
            listOf(TrackRelease().applyChanges(config.versionCodes, config))
        } else {
            track.releases.map {
                if (it.status == config.releaseStatus.publishedName) {
                    it.applyChanges(it.versionCodes.orEmpty() + config.versionCodes, config)
                } else {
                    it
                }
            }
        }

        return track
    }

    private fun createTrackForRollout(config: TrackManager.UpdateConfig): Track {
        val track = publisher.getTrack(editId, config.trackName)

        val keep = track.releases.orEmpty().filterNot(TrackRelease::isRollout)
        track.releases = keep + listOf(TrackRelease().applyChanges(config.versionCodes, config))

        return track
    }

    private fun createDefaultTrack(config: TrackManager.UpdateConfig) = Track().apply {
        track = config.trackName
        releases = listOf(TrackRelease().applyChanges(config.versionCodes, config))
    }

    private fun TrackRelease.applyChanges(
            versionCodes: List<Long>,
            config: TrackManager.UpdateConfig
    ): TrackRelease {
        updateVersionCodes(versionCodes, config.retainableArtifacts)
        updateStatus(config.releaseStatus)
        updateConsoleName(config.releaseName)
        updateReleaseNotes(config.releaseNotes)
        updateUserFraction(config.userFraction)

        return this
    }

    private fun TrackRelease.updateVersionCodes(versionCodes: List<Long>, retainableArtifacts: List<Long>?) {
        this.versionCodes = versionCodes + retainableArtifacts.orEmpty()
    }

    private fun TrackRelease.updateStatus(releaseStatus: ReleaseStatus) {
        status = releaseStatus.publishedName
    }

    private fun TrackRelease.updateConsoleName(releaseName: String?) {
        name = releaseName
    }

    private fun TrackRelease.updateReleaseNotes(rawReleaseNotes: Map<String, String?>) {
        val releaseNotes = rawReleaseNotes.map {
            LocalizedText().apply {
                language = it.key
                text = it.value
            }
        }
        val existingReleaseNotes = this.releaseNotes.orEmpty()

        this.releaseNotes = if (existingReleaseNotes.isEmpty()) {
            releaseNotes
        } else {
            val merged = releaseNotes.toMutableList()

            for (existing in existingReleaseNotes) {
                if (merged.none { it.language == existing.language }) merged += existing
            }

            merged
        }
    }

    private fun TrackRelease.updateUserFraction(userFraction: Double) {
        this.userFraction = userFraction.takeIf { isRollout() }
    }
}
