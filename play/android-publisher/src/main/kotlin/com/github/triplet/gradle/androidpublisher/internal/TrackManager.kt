package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease

internal interface TrackManager {
    fun findMaxAppVersionCode(): Long

    fun getReleaseNotes(): Map<String, List<LocalizedText>>

    fun update(config: UpdateConfig)

    fun promote(config: PromoteConfig)

    data class BaseConfig(
            val releaseStatus: ReleaseStatus?,
            val userFraction: Double?,
            val releaseNotes: Map<String, String?>?,
            val retainableArtifacts: List<Long>?,
            val releaseName: String?
    )

    data class UpdateConfig(
            val trackName: String,
            val versionCodes: List<Long>,
            val releaseStatus: ReleaseStatus,
            val didPreviousBuildSkipCommit: Boolean,
            val base: BaseConfig
    )

    data class PromoteConfig(
            val promoteTrackName: String,
            val fromTrackName: String?,
            val base: BaseConfig
    )
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

    override fun getReleaseNotes(): Map<String, List<LocalizedText>> {
        val releaseNotes = mutableMapOf<String, List<LocalizedText>>()

        val tracks = publisher.listTracks(editId)
        for (track in tracks) {
            val notes = track.releases?.maxBy {
                it.versionCodes?.max() ?: Long.MIN_VALUE
            }?.releaseNotes.orEmpty()

            releaseNotes[track.track] = notes
        }

        return releaseNotes
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

    override fun promote(config: TrackManager.PromoteConfig) {
        val activeTracks = publisher.listTracks(editId).filter {
            it.releases.orEmpty().flatMap { it.versionCodes.orEmpty() }.isNotEmpty()
        }
        check(activeTracks.isNotEmpty()) { "Nothing to promote. Did you mean to run publish?" }

        // Find the target track
        val track = if (config.fromTrackName == null) {
            // Get the track with the highest version code
            activeTracks.sortedByDescending {
                it.releases.flatMap { it.versionCodes.orEmpty() }.max()
            }.first()
        } else {
            checkNotNull(activeTracks.find { it.track.equals(config.fromTrackName, true) }) {
                "${config.fromTrackName.capitalize()} track has no active artifacts"
            }
        }

        // Update the track
        for (release in track.releases) {
            release.mergeChanges(null, config.base)
        }
        // Only keep the unique statuses from the highest version code since duplicate statuses are
        // not allowed. This is how we deal with an update from inProgress -> completed. We update
        // all the tracks to completed, then get rid of the one that used to be inProgress.
        track.releases = track.releases.sortedByDescending {
            it.versionCodes?.max()
        }.distinctBy {
            it.status
        }

        println("Promoting release from track '${track.track}'")
        track.track = config.promoteTrackName
        publisher.updateTrack(editId, track)
    }

    private fun createTrackForSkippedCommit(config: TrackManager.UpdateConfig): Track {
        val track = publisher.getTrack(editId, config.trackName)

        if (track.releases.isNullOrEmpty()) {
            track.releases = listOf(TrackRelease().mergeChanges(config.versionCodes, config.base))
        } else {
            val hasReleaseToBeUpdated = track.releases.firstOrNull {
                it.status == config.releaseStatus.publishedName
            } != null

            if (hasReleaseToBeUpdated) {
                for (release in track.releases) {
                    if (release.status == config.releaseStatus.publishedName) {
                        release.mergeChanges(
                                release.versionCodes.orEmpty() + config.versionCodes, config.base)
                    }
                }
            } else {
                track.releases = track.releases +
                        listOf(TrackRelease().mergeChanges(config.versionCodes, config.base))
            }
        }

        return track
    }

    private fun createTrackForRollout(config: TrackManager.UpdateConfig): Track {
        val track = publisher.getTrack(editId, config.trackName)

        val keep = track.releases.orEmpty().filterNot { it.isRollout() }
        track.releases = keep + listOf(TrackRelease().mergeChanges(config.versionCodes, config.base))

        return track
    }

    private fun createDefaultTrack(config: TrackManager.UpdateConfig) = Track().apply {
        track = config.trackName
        releases = listOf(TrackRelease().mergeChanges(config.versionCodes, config.base))
    }

    private fun TrackRelease.mergeChanges(
            versionCodes: List<Long>?,
            config: TrackManager.BaseConfig
    ) = apply {
        updateVersionCodes(versionCodes, config.retainableArtifacts)
        updateStatus(config.releaseStatus)
        updateConsoleName(config.releaseName)
        updateReleaseNotes(config.releaseNotes)
        updateUserFraction(config.userFraction)
    }

    private fun TrackRelease.updateVersionCodes(versionCodes: List<Long>?, retainableArtifacts: List<Long>?) {
        val newVersions = versionCodes ?: this.versionCodes.orEmpty()
        this.versionCodes = newVersions + retainableArtifacts.orEmpty()
    }

    private fun TrackRelease.updateStatus(releaseStatus: ReleaseStatus?) {
        if (releaseStatus != null) status = releaseStatus.publishedName
    }

    private fun TrackRelease.updateConsoleName(releaseName: String?) {
        if (releaseName != null) name = releaseName
    }

    private fun TrackRelease.updateReleaseNotes(rawReleaseNotes: Map<String, String?>?) {
        val releaseNotes = rawReleaseNotes.orEmpty().map { (locale, notes) ->
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

    private fun TrackRelease.updateUserFraction(userFraction: Double?) {
        if (userFraction != null) {
            this.userFraction = userFraction.takeIf { isRollout() }
        }
    }

    private fun ReleaseStatus.isRollout() =
            this == ReleaseStatus.IN_PROGRESS || this == ReleaseStatus.HALTED

    private fun TrackRelease.isRollout() =
            status == ReleaseStatus.IN_PROGRESS.publishedName ||
                    status == ReleaseStatus.HALTED.publishedName
}
