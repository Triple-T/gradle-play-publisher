package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.orNull
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal interface ExtensionOptionsBase {
    @get:Nested val extension: PlayPublisherExtension
}

internal interface ArtifactExtensionOptions : ExtensionOptionsBase {
    @get:Internal
    @set:Option(
            option = "artifact-dir",
            description = "Set the prebuilt artifact (APKs/App Bundles) directory"
    )
    var artifactDirOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            val dir = File(value)
            extension.artifactDir = requireNotNull(dir.orNull()) {
                "Folder '${dir.absolutePath}' does not exist."
            }.absoluteFile
        }
}

internal interface WriteTrackExtensionOptions : ExtensionOptionsBase {
    @get:Internal
    @set:Option(
            option = "no-commit",
            description = "Don't commit changes from this build."
    )
    var noCommitOption: Boolean
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.commit = !value
        }
}

internal interface TrackExtensionOptions : WriteTrackExtensionOptions {
    @get:Internal
    @set:Option(
            option = "track",
            description = "Set the track in which to upload your app."
    )
    var trackOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.track = value
        }

    @get:Internal
    @set:Option(
            option = "user-fraction",
            description = "Set the user fraction intended to receive an 'inProgress' release. " +
                    "Ex: 0.1 == 10%"
    )
    var userFractionOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.userFraction = value.toDouble()
        }

    @get:OptionValues("release-status")
    val releaseStatusOptions
        get() = ReleaseStatus.values().map { it.publishedName }
    @get:Internal
    @set:Option(
            option = "release-status",
            description = "Set the app release status."
    )
    var releaseStatusOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.releaseStatus = value
        }
}

interface TransientTrackOptions {
    @get:Optional
    @get:Input
    @set:Option(
            option = "release-name",
            description = "Set the Play Console developer facing release name."
    )
    var releaseName: String?

    class Holder : TransientTrackOptions {
        override var releaseName: String? = null
    }
}

internal interface UpdatableTrackExtensionOptions : TrackExtensionOptions {
    @get:Internal
    @set:Option(
            option = "from-track",
            description = "Set the track from which to promote a release."
    )
    var fromTrackOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.fromTrack = value
        }
    @get:Internal
    @set:Option(
            option = "update",
            description = "Set the track to update when promoting releases. This is the same as " +
                    "using 'from-track' and 'track' with the same value."
    )
    var updateTrackOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.fromTrack = value
            extension.track = value
        }
}

internal interface PublishableTrackExtensionOptions : TrackExtensionOptions,
        ArtifactExtensionOptions {
    @get:OptionValues("resolution-strategy")
    val resolutionStrategyOptions
        get() = ResolutionStrategy.values().map { it.publishedName }
    @get:Internal
    @set:Option(
            option = "resolution-strategy",
            description = "Set the version conflict resolution strategy."
    )
    var resolutionStrategyOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.resolutionStrategy = value
        }
}

internal interface GlobalPublishableArtifactExtensionOptions : PublishableTrackExtensionOptions {
    @get:Internal
    @set:Option(
            option = "default-to-app-bundles",
            description = "Prioritize App Bundles over APKs where applicable."
    )
    var defaultToAppBundlesOption: Boolean
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.defaultToAppBundles = value
        }
}

interface BootstrapOptions {
    @get:Internal
    @set:Option(
            option = "app-details",
            description = "Download app details such as your contact email."
    )
    var downloadAppDetails: Boolean

    @get:Internal
    @set:Option(
            option = "listings",
            description = "Download listings for each language such as your app title and graphics."
    )
    var downloadListings: Boolean

    @get:Internal
    @set:Option(option = "release-notes", description = "Download release notes for each language.")
    var downloadReleaseNotes: Boolean

    @get:Internal
    @set:Option(option = "products", description = "Download in-app purchases and subscriptions.")
    var downloadProducts: Boolean

    class Holder : BootstrapOptions {
        private val isRequestingSpecificFeature = AtomicBoolean()

        override var downloadAppDetails = true
            set(value) {
                onSet()
                field = value
            }
        override var downloadListings = true
            set(value) {
                onSet()
                field = value
            }
        override var downloadReleaseNotes = true
            set(value) {
                onSet()
                field = value
            }
        override var downloadProducts = true
            set(value) {
                onSet()
                field = value
            }

        /**
         * By default, we download all features. However, if they are specified with CLI options, we
         * only download those features.
         *
         * Note: this method must be called before updating the field since it may overwrite them.
         */
        private fun onSet() {
            if (isRequestingSpecificFeature.compareAndSet(false, true)) {
                downloadAppDetails = false
                downloadListings = false
                downloadReleaseNotes = false
                downloadProducts = false
            }
        }
    }
}
