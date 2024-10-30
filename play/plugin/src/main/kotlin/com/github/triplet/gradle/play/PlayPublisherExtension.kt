package com.github.triplet.gradle.play

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.configurationcache.problems.PropertyTrace
import javax.inject.Inject

/** The entry point for all GPP related configuration. */
abstract class PlayPublisherExtension @Inject constructor(
        private val name: String,
) : Named {
    @Internal
    override fun getName(): String = name

    /**
     * Enables or disables GPP.
     *
     * Defaults to `true`.
     */
    @get:Input
    abstract val enabled: Property<Boolean>

    /**
     * JSON Service Account authentication file. You can also specify credentials through the
     * `ANDROID_PUBLISHER_CREDENTIALS` environment variable.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputFile
    abstract val serviceAccountCredentials: RegularFileProperty

    /**
     * Use GCP Application Default Credentials.
     */
    @get:Optional
    @get:Input
    abstract val useApplicationDefaultCredentials: Property<Boolean>

    /**
     * Specify the Service Account to impersonate
     */
    @get:Optional
    @get:Input
    abstract val impersonateServiceAccount: Property<String>

    /**
     * Choose the default packaging method. Either App Bundles or APKs. Affects tasks like
     * `publish`.
     *
     * Defaults to `false` because App Bundles require Google Play App Signing to be configured.
     */
    @get:Input
    abstract val defaultToAppBundles: Property<Boolean>

    /**
     * Choose whether or not to apply the changes from this build. Defaults to true.
     */
    @get:Input
    abstract val commit: Property<Boolean>

    /**
     * Specify the track from which to promote a release. That is, the specified track will be
     * promoted to [track].
     *
     * See [track] for valid values. The default is determined dynamically from the most unstable
     * release available for promotion. That is, if there is a stable release and an alpha release,
     * the alpha will be chosen.
     */
    @get:Optional
    @get:Input
    abstract val fromTrack: Property<String>

    /**
     * Specify the track in which to upload your app.
     *
     * May be one of `internal`, `alpha`, `beta`, `production`, or a custom track. Defaults to
     * `internal`.
     */
    @get:Input
    abstract val track: Property<String>

    /**
     * Specify the track to promote a release to.
     *
     * See [track] for valid values. If no promote track is specified, [track] is used instead.
     */
    @get:Optional
    @get:Input
    abstract val promoteTrack: Property<String>

    /**
     * Specify the version code to promote from [fromTrack] to [track].
     * Default is the highest version code in [fromTrack].
     */
    @get:Optional
    @get:Input
    abstract val versionCode: Property<Long>

    /**
     * Specify the initial user fraction intended to receive an `inProgress` release. Defaults to
     * 0.1 (10%).
     *
     * @see releaseStatus
     */
    @get:Optional
    @get:Input
    abstract val userFraction: Property<Double>

    /**
     * Specify the update priority for your release. For information on consuming this value, take
     * a look at
     * [Google's documentation](https://developer.android.com/guide/playcore/in-app-updates).
     * Defaults to API value.
     */
    @get:Optional
    @get:Input
    abstract val updatePriority: Property<Int>

    /**
     * Specify the status to apply to the uploaded app release.
     *
     * Defaults to [ReleaseStatus.COMPLETED].
     */
    @get:Optional
    @get:Input
    abstract val releaseStatus: Property<ReleaseStatus>

    /** Specify the Play Console developer facing release name. */
    @get:Optional
    @get:Input
    abstract val releaseName: Property<String>

    /**
     * Specify the resolution strategy to employ when a version conflict occurs.
     *
     * Defaults to [ResolutionStrategy.FAIL].
     */
    @get:Input
    abstract val resolutionStrategy: Property<ResolutionStrategy>

    /**
     * Specify a directory where prebuilt artifacts such as APKs or App Bundles may be found. The
     * directory must exist and should contain only artifacts intended to be uploaded. If no
     * directory is specified, your app will be built on-the-fly when you try to publish it.
     *
     * Defaults to null (i.e. your app will be built pre-publish).
     */
    @get:Internal("Directory mapped to a useful set of files later")
    abstract val artifactDir: DirectoryProperty

    /**
     * @return the configuration for your app's retainable objects such as previous artifacts and
     * OBB files.
     */
    @get:Nested
    abstract val retain: Retain

    /** Configure your app's retainable objects such as previous artifacts and OBB files. */
    @Suppress("unused") // Public API
    fun retain(action: Action<Retain>) {
        action.execute(retain)
    }

    /** Entry point for retainable artifact configuration. */
    abstract class Retain {
        /** Specify the version code(s) of an APK or App Bundle to retain. Defaults to none. */
        @get:Optional
        @get:Input
        abstract val artifacts: ListProperty<Long>

        /**
         * Specify the reference version of the main OBB file to attach to an APK.
         *
         * Defaults to none.
         * @see patchObb
         */
        @get:Optional
        @get:Input
        abstract val mainObb: Property<Int>

        /**
         * Specify the reference version of the patch OBB file to attach to an APK.
         *
         * Defaults to none.
         * @see mainObb
         */
        @get:Optional
        @get:Input
        abstract val patchObb: Property<Int>
    }
}
