package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues

internal interface ExtensionOptions {
    @get:Nested val extension: PlayPublisherExtension

    @get:OptionValues("track")
    val trackOptions
        get() = TrackType.values().map { it.publishedName }
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
            description = "Set the user percent intended to receive a 'rollout' update. 10% == 0.1"
    )
    var userFractionOption: String
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.userFraction = value.toDouble()
        }

    @get:Internal
    @set:Option(
            option = "trim-on-size-limit",
            description = "Trim listing details instead of failing should they be too large."
    )
    var trimOnSizeLimitOption: Boolean
        get() = throw UnsupportedOperationException()
        set(value) {
            extension.errorOnSizeLimit = !value
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
            extension.resolutionStrategy = value
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
