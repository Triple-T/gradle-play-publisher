package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Nested
import javax.inject.Inject

internal abstract class WriteTrackLifecycleTask @Inject constructor(
        @get:Nested val extension: PlayPublisherExtension,
        executionDir: Directory,
) : DefaultTask(), WriteTrackExtensionOptions by CliOptionsImpl(extension, executionDir)

internal abstract class UpdatableTrackLifecycleTask @Inject constructor(
        @get:Nested val extension: PlayPublisherExtension,
        executionDir: Directory,
) : DefaultTask(), UpdatableTrackExtensionOptions by CliOptionsImpl(extension, executionDir)

internal abstract class PublishableTrackLifecycleTask @Inject constructor(
        @get:Nested val extension: PlayPublisherExtension,
        executionDir: Directory,
) : DefaultTask(), PublishableTrackExtensionOptions by CliOptionsImpl(extension, executionDir)

internal abstract class GlobalPublishableArtifactLifecycleTask @Inject constructor(
        @get:Nested val extension: PlayPublisherExtension,
        executionDir: Directory,
) : DefaultTask(),
        GlobalPublishableArtifactExtensionOptions by CliOptionsImpl(extension, executionDir)

internal abstract class BootstrapLifecycleTask @Inject constructor(
        optionsHolder: BootstrapOptions.Holder,
) : DefaultTask(), BootstrapOptions by optionsHolder
