package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.TrackType
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.github.triplet.gradle.play.internal.trackOrDefault
import com.github.triplet.gradle.play.internal.validatedTrack
import org.gradle.api.Action
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

@Suppress("PropertyName")
open class PlayPublisherExtension @JvmOverloads constructor(
        @get:Internal internal val name: String = "default" // Needed for Gradle
) {
    @get:Internal("Backing property for public input")
    internal var _serviceAccountCredentials: File? = null
    /**
     * Service Account authentication file. Json is preferred, but PKCS12 is also supported. For
     * PKCS12 to work, the [serviceAccountEmail] must be specified.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    var serviceAccountCredentials
        get() = _serviceAccountCredentials
        set(value) {
            _serviceAccountCredentials = value
        }

    @get:Internal("Backing property for public input")
    internal var _serviceAccountEmail: String? = null
    /** Service Account email. Only needed if PKCS12 credentials are used. */
    @get:Optional
    @get:Input
    var serviceAccountEmail
        get() = _serviceAccountEmail
        set(value) {
            _serviceAccountEmail = value
        }

    @get:Internal("Backing property for public input")
    internal var _defaultToAppBundles: Boolean? = null
    /**
     * Choose the default packaging method. Either App Bundles or APKs. Affects tasks like
     * `publish`.
     *
     * Defaults to false because App Bundles require Google Play App Signing to be configured.
     */
    @get:Input
    var defaultToAppBundles
        get() = _defaultToAppBundles ?: false
        set(value) {
            _defaultToAppBundles = value
        }

    @get:Internal("Backing property for public input")
    internal var _fromTrack: TrackType? = null
    /**
     * Specify the track from which to promote a release. That is, the specified track will be
     * promoted to [track].
     *
     * See [track] for valid values. The default is determined dynamically from the most unstable
     * release available for promotion. That is, if there is a stable release and an alpha release,
     * the alpha will be chosen.
     */
    @get:Input
    var fromTrack
        get() = (_fromTrack ?: TrackType.INTERNAL).publishedName
        set(value) {
            _fromTrack = validatedTrack(value)
        }

    @get:Internal("Backing property for public input")
    internal var _track: TrackType? = null
    /**
     * Specify the track in which to upload your app.
     *
     * May be one of internal, alpha, beta, rollout, or production. Default is internal.
     */
    @get:Input
    var track
        get() = trackOrDefault.publishedName
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
    internal var _resolutionStrategy: ResolutionStrategy? = null
    /**
     * Specify the resolution strategy to employ when a version conflict occurs.
     *
     * May be one of auto, fail, or ignore. Default is ignore.
     */
    @get:Input
    var resolutionStrategy
        get() = resolutionStrategyOrDefault.publishedName
        set(value) {
            _resolutionStrategy = requireNotNull(
                    ResolutionStrategy.values().find { it.publishedName == value }
            ) {
                "Resolution strategy must be one of " +
                        ResolutionStrategy.values().joinToString { "'${it.publishedName}'" }
            }
        }

    @get:Internal("ProcessPackageMetadata is always out-of-date. Also, Closures with " +
                          "parameters cannot be used as inputs.")
    internal var _outputProcessor: Action<ApkVariantOutput>? = null

    /**
     * If the [resolutionStrategy] is auto, provide extra processing on top of what this plugin
     * already does. For example, you could update each output's version name using the newly
     * mutated version codes.
     *
     * Note: by the time the output is received, its version code will have been linearly shifted
     * such that the smallest output's version code is 1 unit greater than the maximum version code
     * found in the Play Store.
     */
    @Suppress("unused") // Public API
    fun outputProcessor(processor: Action<ApkVariantOutput>) {
        _outputProcessor = processor
    }

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
            val status = _releaseStatus ?: if (trackOrDefault == TrackType.ROLLOUT) {
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
