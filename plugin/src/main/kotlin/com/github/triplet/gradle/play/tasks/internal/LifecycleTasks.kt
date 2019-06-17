package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import javax.inject.Inject

internal open class ArtifactLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), ArtifactExtensionOptions

internal open class WriteTrackLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), WriteTrackExtensionOptions

internal open class UpdatableTrackLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        optionsHolder: TransientTrackOptions.Holder
) : DefaultTask(), UpdatableTrackExtensionOptions, TransientTrackOptions by optionsHolder

internal open class PublishableTrackLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        optionsHolder: TransientTrackOptions.Holder
) : DefaultTask(), PublishableTrackExtensionOptions, TransientTrackOptions by optionsHolder

internal open class GlobalPublishableArtifactLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), GlobalPublishableArtifactExtensionOptions

internal open class BootstrapLifecycleTask @Inject constructor(
        optionsHolder: BootstrapOptions.Holder
) : DefaultTask(), BootstrapOptions by optionsHolder
