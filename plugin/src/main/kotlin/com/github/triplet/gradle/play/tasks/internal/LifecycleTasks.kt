package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import javax.inject.Inject

internal open class WriteLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), WriteExtensionOptions

internal open class UpdatableArtifactLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), UpdatableArtifactExtensionOptions

internal open class PublishableArtifactLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), PublishableArtifactExtensionOptions

internal open class GlobalPublishableArtifactLifecycleTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), GlobalPublishableArtifactExtensionOptions

internal open class BootstrapLifecycleTask : DefaultTask(),
        BootstrapOptions by BootstrapOptions.Holder
