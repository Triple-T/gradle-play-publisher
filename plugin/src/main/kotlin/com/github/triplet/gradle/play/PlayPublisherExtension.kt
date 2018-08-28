package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.AccountConfig
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.TrackType
import com.github.triplet.gradle.play.internal.validatedTrack
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class PlayPublisherExtension : AccountConfig by PlayAccountConfigExtension() {
    /**
     * Choose the default packaging method. Either App Bundles or APKs. Affects tasks like
     * `publish`.
     */
    @get:Input
    var defaultToAppBundles = false // App Bundles require Google Play App Signing

    @get:Internal("Backing property for public input")
    internal var _fromTrack: TrackType? = null
    /**
     * Specify the track from which to promote a release. That is, the specified track will be
     * promoted to [track].
     *
     * See [track] for valid values.
     */
    @get:Input
    var fromTrack
        get() = (_fromTrack ?: TrackType.INTERNAL).publishedName
        set(value) {
            _fromTrack = validatedTrack(value)
        }

    @get:Internal("Backing property for public input")
    internal var _track = TrackType.INTERNAL
    /**
     * Specify the track in which to upload your app.
     *
     * May be one of internal, alpha, beta, rollout, or production. Default is internal.
     */
    @get:Input
    var track
        get() = _track.publishedName
        set(value) {
            _track = validatedTrack(value)
        }

    @get:Internal("Backing property for public input")
    internal var _userFraction: Double? = null
    /**
     * Specify the initial user percent intended to receive a 'rollout' update (see [track]).
     * Default is 10% == 0.1.
     */
    @get:Input
    var userFraction: Double
        get() = _userFraction ?: 0.1
        set(value) {
            _userFraction = value
        }

    @get:Internal("Backing property for public input")
    internal var _resolutionStrategy = ResolutionStrategy.FAIL
    /**
     * Specify the resolution strategy to employ when a version conflict occurs.
     *
     * May be one of auto, fail, or ignore. Default is ignore.
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
    internal var _releaseStatus: ReleaseStatus? = null
    /**
     * Specify the status to apply to the uploaded app release.
     *
     * May be one of completed, draft, halted, or inProgress. Default is completed for all tracks
     * except rollout where inProgress is the default.
     */
    @get:Input
    var releaseStatus: String
        get() {
            val status = _releaseStatus ?: if (_track == TrackType.ROLLOUT) {
                ReleaseStatus.IN_PROGRESS
            } else {
                ReleaseStatus.COMPLETED
            }

            return status.publishedName
        }
        set(value) {
            _releaseStatus = requireNotNull(
                    ReleaseStatus.values().find { it.publishedName == value }
            ) {
                "Release Status must be one of " +
                        ReleaseStatus.values().joinToString { "'${it.publishedName}'" }
            }
        }
}
