package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.AccountConfig
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.TrackType

open class PlayPublisherExtension : AccountConfig by PlayAccountConfigExtension() {
    /**
     * Used to give feedback for users setting incompatible status/track combos.
     */
    private var releaseTrackSet = false

    /**
     * Release statuses that are compatible with a [releaseStatus] of `rollout`
     */
    private var rolloutStatuses = listOf(ReleaseStatus.INPROGRESS, ReleaseStatus.HALTED)

    /**
     * Check the compatibility of [track] and [releaseStatus]
     * For reference: [https://developers.google.com/android-publisher/api-ref/edits/tracks]
     */
    private fun checkTrackCompatibility() {
        if ((_track != TrackType.ROLLOUT && rolloutStatuses.contains(_releaseStatus))
            || (_track == TrackType.ROLLOUT && !rolloutStatuses.contains(_releaseStatus)))
            throw IllegalArgumentException("Incompatible track and releaseStatus specified.")
    }

    internal var _track = TrackType.INTERNAL
    /**
     * Specify the track in which to upload your app. May be one of internal, alpha, beta, rollout,
     * or production. Default is internal.
     */
    var track
        get() = _track.publishedName
        set(value) {
            _track = requireNotNull(TrackType.values().find { it.name.equals(value, true) }) {
                "Track must be one of ${TrackType.values().joinToString { "'${it.publishedName}'" }}"
            }
            if (releaseTrackSet)
                checkTrackCompatibility()
            else if (_track == TrackType.ROLLOUT)
                _releaseStatus = ReleaseStatus.INPROGRESS
            releaseTrackSet = true
        }
    /**
     * Choose whether or not to untrack superseded versions automatically. See
     * https://github.com/Triple-T/gradle-play-publisher#untrack-conflicting-versions. Disabled by
     * default.
     */
    var untrackOld = false
    /**
     * Specify the initial user percent intended to receive a 'rollout' update (see [track]).
     * Default is 10% == 0.1.
     */
    var userFraction = 0.1

    /**
     * Choose whether or not to upload images when publishing the Play Store listing. Disabled by
     * default for performance reasons.
     */
    var uploadImages = false
    /**
     * Choose whether or not to throw an error should a Play Store listing detail be too large or
     * simply trim it. Default throws.
     */
    var errorOnSizeLimit = true

    internal var _releaseStatus = ReleaseStatus.COMPLETED
    /**
     * Specify the status to apply to the uploaded app release. May be one of completed, draft,
     * halted, or inProgress. Default is completed.
     */
    var releaseStatus: String
        get() = _releaseStatus.status
        set(value) {
            _releaseStatus = requireNotNull(ReleaseStatus.values().find { it.name.equals(value, true) }) {
                "Release Status must be one of ${ReleaseStatus.values().joinToString { "'${it.status}'" }}"
            }
            if (releaseTrackSet)
                checkTrackCompatibility()
            else if (rolloutStatuses.contains(_releaseStatus))
                _track = TrackType.ROLLOUT
            releaseTrackSet = true
        }
}
