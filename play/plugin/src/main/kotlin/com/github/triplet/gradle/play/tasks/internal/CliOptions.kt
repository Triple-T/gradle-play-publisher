package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.PlayExtensionConfig
import com.github.triplet.gradle.play.internal.textToReleaseStatus
import com.github.triplet.gradle.play.internal.textToResolutionStrategy
import com.github.triplet.gradle.play.internal.updateProperty
import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import java.util.concurrent.atomic.AtomicBoolean

internal interface ExtensionOptionsBase {
    @get:Nested
    val extension: PlayPublisherExtension
}

internal interface ArtifactExtensionOptions : ExtensionOptionsBase {
    @Internal
    fun getProject(): Project

    @get:Internal
    @set:Option(
            option = "artifact-dir",
            description = "Set the prebuilt artifact (APKs/App Bundles) directory"
    )
    var artifactDirOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            val dir = getProject().rootProject.file(value)
            val f = requireNotNull(dir.orNull()) {
                "Folder '$dir' does not exist."
            }
            extension.updateProperty(PlayExtensionConfig::artifactDir, f, force = true)
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
            extension.updateProperty(PlayExtensionConfig::commit, !value, force = true)
        }
}

internal interface TrackExtensionOptions : WriteTrackExtensionOptions {
    @get:Internal
    @set:Option(
            option = "user-fraction",
            description = "Set the user fraction intended to receive an 'inProgress' release. " +
                    "Ex: 0.1 == 10%"
    )
    var userFractionOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.updateProperty(
                    PlayExtensionConfig::userFraction, value.toDouble(), force = true)
        }

    @get:Internal
    @set:Option(
            option = "update-priority",
            description = "Set the update priority for your release."
    )
    var updatePriorityOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.updateProperty(
                    PlayExtensionConfig::updatePriority, value.toInt(), force = true)
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
            extension.updateProperty(
                    PlayExtensionConfig::releaseStatus, textToReleaseStatus(value), force = true)
        }

    @get:Internal
    @set:Option(
            option = "release-name",
            description = "Set the Play Console developer facing release name."
    )
    var releaseName: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.updateProperty(PlayExtensionConfig::releaseName, value, force = true)
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
            extension.updateProperty(PlayExtensionConfig::fromTrack, value, force = true)
        }

    @get:Internal
    @set:Option(
            option = "promote-track",
            description = "Set the track to promote a release to."
    )
    var promoteTrackOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.updateProperty(PlayExtensionConfig::promoteTrack, value, force = true)
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
            fromTrackOption = value
            promoteTrackOption = value
        }
}

internal interface PublishableTrackExtensionOptions : TrackExtensionOptions,
        ArtifactExtensionOptions {
    @get:Internal
    @set:Option(
            option = "track",
            description = "Set the track in which to upload your app."
    )
    var trackOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.updateProperty(PlayExtensionConfig::track, value, force = true)
        }

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
            extension.updateProperty(
                    PlayExtensionConfig::resolutionStrategy,
                    textToResolutionStrategy(value),
                    force = true
            )
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
            extension.updateProperty(PlayExtensionConfig::defaultToAppBundles, value, force = true)
        }
}

internal interface BootstrapOptions {
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
