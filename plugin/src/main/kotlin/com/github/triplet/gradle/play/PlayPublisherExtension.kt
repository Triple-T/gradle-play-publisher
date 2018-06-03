package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.AccountConfig
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.TrackType
import java.io.File

open class PlayPublisherExtension : AccountConfig by PlayAccountConfigExtension() {
    internal var _track = TrackType.INTERNAL
    /**
     * Specify the track in which to upload your app. May be one of internal, alpha, beta, rollout,
     * or production. Default is internal.
     */
    var track
        get() = _track.publishedName
        set(value) {
            _track = requireNotNull(TrackType.values().find { it.publishedName == value }) {
                "Track must be one of ${TrackType.values().joinToString { "'${it.publishedName}'" }}"
            }
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

    internal lateinit var _releaseStatus: ReleaseStatus
    /**
     * Specify the status to apply to the uploaded app release. May be one of completed, draft,
     * halted, or inProgress. Default is completed for all tracks except rollout where inProgress is
     * the default.
     */
    var releaseStatus
        get() = when {
            ::_releaseStatus.isInitialized -> _releaseStatus
            _track == TrackType.ROLLOUT -> ReleaseStatus.IN_PROGRESS
            else -> ReleaseStatus.COMPLETED
        }.publishedName
        set(value) {
            _releaseStatus = requireNotNull(
                    ReleaseStatus.values().find { it.publishedName == value }
            ) {
                "Release Status must be one of " +
                        ReleaseStatus.values().joinToString { "'${it.publishedName}'" }
            }
        }

    /**
     * Optional folder to find APK files. Only for use when the APK is built by an external process
     */
    var overrideBuildOutput: File? = null

}
