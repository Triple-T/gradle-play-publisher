package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.AccountConfig
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.TrackType
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class PlayPublisherExtension : AccountConfig by PlayAccountConfigExtension() {
    @get:Internal("Backing property for public input")
    internal var _track = TrackType.INTERNAL
    /**
     * Specify the track in which to upload your app. May be one of internal, alpha, beta, rollout,
     * or production. Default is internal.
     */
    @get:Input
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
    @get:Input
    var untrackOld = false
    /**
     * Specify the initial user percent intended to receive a 'rollout' update (see [track]).
     * Default is 10% == 0.1.
     */
    @get:Input
    var userFraction = 0.1
    /**
     * Choose whether or not to throw an error should a Play Store listing detail be too large or
     * simply trim it. Default throws.
     */
    @get:Input
    var errorOnSizeLimit = true

    @get:Internal("Backing property for public input")
    internal var _resolutionStrategy = ResolutionStrategy.FAIL
    /**
     * Specify the resolution strategy to employ when a version conflict occurs. May be one of auto,
     * fail, or ignore. Default is ignore.
     */
    @get:Input
    var resolutionStrategy
        get() = _resolutionStrategy.publishedName
        set(value) {
            _resolutionStrategy = requireNotNull(
                    ResolutionStrategy.values().find { it.publishedName == value }
            ) {
                "Resolution strategy must be one of " +
                        ResolutionStrategy.values().joinToString { "'${it.publishedName}'" }
            }
        }
    /**
     * If the [resolutionStrategy] is auto, optionally compute a new version name from the updated
     * version code. Returning null means the version name should not be changed.
     */
    @get:Internal("ProcessPackageMetadataTask is always out-of-date. Also, Closures with " +
                          "parameters cannot be used as inputs.")
    var versionNameOverride: (versionCode: Int) -> String? = { null }

    @get:Internal("Backing property for public input")
    internal lateinit var _releaseStatus: ReleaseStatus
    /**
     * Specify the status to apply to the uploaded app release. May be one of completed, draft,
     * halted, or inProgress. Default is completed for all tracks except rollout where inProgress is
     * the default.
     */
    @get:Input
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
}
