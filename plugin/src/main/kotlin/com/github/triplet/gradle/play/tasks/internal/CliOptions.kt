package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.orNull
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal interface ExtensionOptionsBase {
    @get:Nested val extension: PlayPublisherExtension
}

internal interface WriteExtensionOptions : ExtensionOptionsBase {
    @get:Internal
    @set:Option(
            option = "skip-commit",
            description = "Don't commit changes from this build."
    )
    var skipCommitOption: Boolean
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.commit = !value
        }
}

internal interface ArtifactExtensionOptions : WriteExtensionOptions {
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

internal interface UpdatableArtifactExtensionOptions : ArtifactExtensionOptions {
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
}

internal interface PublishableArtifactExtensionOptions : ArtifactExtensionOptions {
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

    @get:Internal
    @set:Option(
            option = "artifact-dir",
            description = "Set the prebuilt artifacts (APKs/App Bundles) directory"
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

internal interface GlobalPublishableArtifactExtensionOptions : PublishableArtifactExtensionOptions {
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

    object Holder : BootstrapOptions {
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
         * Since this is an object, it won't be destroyed across Gradle invocations. Therefore, we need
         * to manually reset it every time our plugin is applied.
         */
        fun reset() {
            downloadAppDetails = true
            downloadListings = true
            downloadReleaseNotes = true
            downloadProducts = true

            // Must come after to override onSet
            isRequestingSpecificFeature.set(false)
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
