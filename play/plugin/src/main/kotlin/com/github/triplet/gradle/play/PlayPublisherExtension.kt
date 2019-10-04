package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.commitOrDefault
import com.github.triplet.gradle.play.internal.promoteTrackOrDefault
import com.github.triplet.gradle.play.internal.releaseStatusOrDefault
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.github.triplet.gradle.play.internal.trackOrDefault
import com.github.triplet.gradle.play.internal.userFractionOrDefault
import org.gradle.api.Action
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.io.Serializable

@Suppress("PropertyName")
open class PlayPublisherExtension @JvmOverloads constructor(
        @get:Internal internal val name: String = "default" // Needed for Gradle
) {
    private val _config = Config()

    @get:Internal
    internal val config
        get() = _config.copy(retain = _config.retain.copy())
    @get:Internal
    internal val serializableConfig
        get() = _config.copy(outputProcessor = null, retain = _config.retain.copy())

    /**
     * Enables or disables GPP.
     *
     * Defaults to `true`.
     */
    @get:Input
    var isEnabled: Boolean
        get() = _config.enabled ?: true
        set(value) {
            _config.enabled = value
        }

    /**
     * Service Account authentication file. Json is preferred, but PKCS12 is also supported. For
     * PKCS12 to work, the [serviceAccountEmail] must be specified.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    var serviceAccountCredentials: File?
        get() = _config.serviceAccountCredentials
        set(value) {
            _config.serviceAccountCredentials = value
        }

    // TODO(#710): remove once support for PKCS12 creds is gone
    /** Service Account email. Only needed if PKCS12 credentials are used. */
    @get:Optional
    @get:Input
    @get:Deprecated("Use JSON credentials instead.")
    @set:Deprecated("Use JSON credentials instead.")
    var serviceAccountEmail: String?
        get() = _config.serviceAccountEmail
        set(value) {
            _config.serviceAccountEmail = value
        }

    /**
     * Choose the default packaging method. Either App Bundles or APKs. Affects tasks like
     * `publish`.
     *
     * Defaults to `false` because App Bundles require Google Play App Signing to be configured.
     */
    @get:Input
    var defaultToAppBundles: Boolean
        get() = _config.defaultToAppBundles ?: false
        set(value) {
            _config.defaultToAppBundles = value
        }

    /**
     * Choose whether or not to apply the changes from this build. Defaults to true.
     */
    @get:Input
    var commit: Boolean
        get() = _config.commitOrDefault
        set(value) {
            _config.commit = value
        }

    /**
     * Specify the track from which to promote a release. That is, the specified track will be
     * promoted to [track].
     *
     * See [track] for valid values. The default is determined dynamically from the most unstable
     * release available for promotion. That is, if there is a stable release and an alpha release,
     * the alpha will be chosen.
     */
    @get:Input
    var fromTrack: String
        get() = _config.fromTrack ?: track
        set(value) {
            _config.fromTrack = value
        }

    /**
     * Specify the track in which to upload your app.
     *
     * May be one of `internal`, `alpha`, `beta`, `production`, or a custom track. Defaults to
     * `internal`.
     */
    @get:Input
    var track: String
        get() = _config.trackOrDefault
        set(value) {
            _config.track = value
        }

    /**
     * Specify the track to promote a release to.
     *
     * See [track] for valid values. If no promote track is specified, [track] is used instead.
     */
    @get:Input
    var promoteTrack: String
        get() = _config.promoteTrackOrDefault
        set(value) {
            _config.promoteTrack = value
        }

    /**
     * Specify the initial user fraction intended to receive an `inProgress` release. Defaults to
     * 0.1 (10%).
     *
     * @see releaseStatus
     */
    @get:Input
    var userFraction: Double
        get() = _config.userFractionOrDefault
        set(value) {
            _config.userFraction = value
        }

    /**
     * Specify the resolution strategy to employ when a version conflict occurs.
     *
     * May be one of `auto`, `fail`, or `ignore`. Defaults to `fail`.
     */
    @get:Input
    var resolutionStrategy: String
        get() = _config.resolutionStrategyOrDefault.publishedName
        set(value) {
            _config.resolutionStrategy = requireNotNull(
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
    @Suppress("unused") // Public API
    fun outputProcessor(processor: Action<ApkVariantOutput>) {
        _config.outputProcessor = processor
    }

    /**
     * Specify the status to apply to the uploaded app release.
     *
     * May be one of `completed`, `draft`, `halted`, or `inProgress`. Defaults to `completed`.
     */
    @get:Input
    var releaseStatus: String
        get() = _config.releaseStatusOrDefault.publishedName
        set(value) {
            _config.releaseStatus = requireNotNull(
                    ReleaseStatus.values().find { it.publishedName == value }
            ) {
                "Release Status must be one of " +
                        ReleaseStatus.values().joinToString { "'${it.publishedName}'" }
            }
        }

    /**
     * Specify a directory where prebuilt artifacts such as APKs or App Bundles may be found. The
     * directory must exist and should contain only artifacts intended to be uploaded. If no
     * directory is specified, your app will be built on-the-fly when you try to publish it.
     *
     * Defaults to null (i.e. your app will be built pre-publish).
     */
    @get:Internal("Directory mapped to a useful set of files later")
    var artifactDir: File?
        get() = _config.artifactDir
        set(value) {
            _config.artifactDir = value
        }

    /**
     * @return the configuration for your app's retainable objects such as previous artifacts and
     * OBB files.
     */
    @get:Nested
    val retain: Retain = _config.retain

    /** Configure your app's retainable objects such as previous artifacts and OBB files. */
    @Suppress("unused") // Public API
    fun retain(action: Action<Retain>) {
        action.execute(_config.retain)
    }

    override fun toString() = "PlayPublisherExtension(name='$name', config=$_config)"

    data class Retain(
            /** Specify the version code(s) of an APK or App Bundle to retain. Defaults to none. */
            @get:Optional
            @get:Input
            var artifacts: List<Long>? = null,

            /**
             * Specify the reference version of the main OBB file to attach to an APK.
             *
             * Defaults to none.
             * @see patchObb
             */
            @get:Optional
            @get:Input
            var mainObb: Int? = null,

            /**
             * Specify the reference version of the patch OBB file to attach to an APK.
             *
             * Defaults to none.
             * @see mainObb
             */
            @get:Optional
            @get:Input
            var patchObb: Int? = null
    ) : Serializable

    internal data class Config(
            var enabled: Boolean? = null,
            var serviceAccountCredentials: File? = null,
            var serviceAccountEmail: String? = null,
            var defaultToAppBundles: Boolean? = null,
            var commit: Boolean? = null,
            var fromTrack: String? = null,
            var track: String? = null,
            var promoteTrack: String? = null,
            var userFraction: Double? = null,
            var resolutionStrategy: ResolutionStrategy? = null,
            var outputProcessor: Action<ApkVariantOutput>? = null,
            var releaseStatus: ReleaseStatus? = null,
            var artifactDir: File? = null,

            val retain: Retain = Retain()
    ) : Serializable
}
