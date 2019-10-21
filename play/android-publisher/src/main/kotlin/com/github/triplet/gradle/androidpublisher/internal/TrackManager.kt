package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease

internal interface TrackManager {
    fun findMaxAppVersionCode(): Long

    data class UpdateConfig(
            val trackName: String,
            val versionCodes: List<Long>,
            val releaseStatus: ReleaseStatus,
            val userFraction: Double,
            val releaseNotes: Map<String, String?>,
            val retainableArtifacts: List<Long>?,
            val releaseName: String?,
            val didPreviousBuildSkipCommit: Boolean
    )

    fun update(config: UpdateConfig)
}

internal class DefaultTrackManager(
        private val publisher: InternalPlayPublisher,
        private val editId: String
) : TrackManager {
    override fun findMaxAppVersionCode(): Long {
        return publisher.listTracks(editId)
                .flatMap { it.releases.orEmpty() }
                .flatMap { it.versionCodes.orEmpty() }
                .max() ?: 1
    }

    override fun update(config: TrackManager.UpdateConfig) {
        val track = if (config.didPreviousBuildSkipCommit) {
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

        if (track.releases.isNullOrEmpty()) {
            track.releases = listOf(TrackRelease().applyChanges(config.versionCodes, config))
        } else {
            val hasReleaseToBeUpdated = track.releases.firstOrNull {
                it.status == config.releaseStatus.publishedName
            } != null

            if (hasReleaseToBeUpdated) {
                for (release in track.releases) {
                    if (release.status == config.releaseStatus.publishedName) {
                        release.applyChanges(
                                release.versionCodes.orEmpty() + config.versionCodes, config)
                    }
                }
            } else {
                track.releases = track.releases +
                        listOf(TrackRelease().applyChanges(config.versionCodes, config))
            }
        }

        return track
    }

    private fun createTrackForRollout(config: TrackManager.UpdateConfig): Track {
        val track = publisher.getTrack(editId, config.trackName)

        val keep = track.releases.orEmpty().filterNot { it.isRollout() }
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
    ) = apply {
        updateVersionCodes(versionCodes, config.retainableArtifacts)
        updateStatus(config.releaseStatus)
        updateConsoleName(config.releaseName)
        updateReleaseNotes(config.releaseNotes)
        updateUserFraction(config.userFraction)
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
        val releaseNotes = rawReleaseNotes.map { (locale, notes) ->
            LocalizedText().apply {
                language = locale
                text = notes
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

    private fun ReleaseStatus.isRollout() =
            this == ReleaseStatus.IN_PROGRESS || this == ReleaseStatus.HALTED

    private fun TrackRelease.isRollout() =
            status == ReleaseStatus.IN_PROGRESS.publishedName ||
                    status == ReleaseStatus.HALTED.publishedName
}
