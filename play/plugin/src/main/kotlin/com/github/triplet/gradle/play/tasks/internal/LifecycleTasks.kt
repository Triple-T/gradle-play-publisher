package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import javax.inject.Inject

internal abstract class WriteTrackLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), WriteTrackExtensionOptions

internal abstract class UpdatableTrackLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), UpdatableTrackExtensionOptions

internal abstract class PublishableTrackLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), PublishableTrackExtensionOptions

internal abstract class GlobalPublishableArtifactLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), GlobalPublishableArtifactExtensionOptions

internal abstract class BootstrapLifecycleTask @Inject constructor(
        optionsHolder: BootstrapOptions.Holder
) : DefaultTask(), BootstrapOptions by optionsHolder
