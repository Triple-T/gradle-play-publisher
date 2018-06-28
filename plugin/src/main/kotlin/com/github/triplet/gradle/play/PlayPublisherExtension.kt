package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
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

    @get:Internal("Backing property for public input")
    internal var _fromTrack: TrackType? = null
    /**
     * Specify the track for your app that will be modified. May be one of internal, alpha, beta, rollout,
     * or production. Default is internal.
     */
    @get:Input
    internal var fromTrack
        get() = _fromTrack?.publishedName
        set(value) {
            if (value.isNullOrBlank()) {
                _fromTrack = null
                return
            }

            _fromTrack = requireNotNull(TrackType.values().find { it.publishedName == value }) {
                "Track to modify must be one of ${TrackType.values().joinToString { "'${it.publishedName}'" }}"
            }
        }

    /**
     * Specify the initial user percent intended to receive a 'rollout' update (see [track]).
     * Default is 10% == 0.1.
     */
    @get:Input
    var userFraction = 0.1

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
     * If the [resolutionStrategy] is auto, provide extra processing on top of what this plugin
     * already does. For example, you could update each output's version name using the newly
     * mutated version codes.
     *
     * Note: by the time the output is received, its version code will have been linearly shifted
     * such that the smallest output's version code is 1 unit greater than the maximum version code
     * found in the Play Store.
     */
    @get:Internal("ProcessPackageMetadata is always out-of-date. Also, Closures with " +
                          "parameters cannot be used as inputs.")
    var outputProcessor: (ApkVariantOutput.() -> Unit)? = null

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
