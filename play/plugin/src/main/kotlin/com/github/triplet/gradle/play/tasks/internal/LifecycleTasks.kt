package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Nested
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class WriteTrackLifecycleTask @Inject constructor(
        @get:Nested val extension: PlayPublisherExtension,
        executionDir: Directory,
) : DefaultTask(), WriteTrackExtensionOptions by CliOptionsImpl(extension, executionDir)

@DisableCachingByDefault
internal abstract class UpdatableTrackLifecycleTask @Inject constructor(
        @get:Nested val extension: PlayPublisherExtension,
        executionDir: Directory,
) : DefaultTask(), UpdatableTrackExtensionOptions by CliOptionsImpl(extension, executionDir)

@DisableCachingByDefault
internal abstract class PublishableTrackLifecycleTask @Inject constructor(
        @get:Nested val extension: PlayPublisherExtension,
        executionDir: Directory,
) : DefaultTask(), PublishableTrackExtensionOptions by CliOptionsImpl(extension, executionDir)

@DisableCachingByDefault
internal abstract class GlobalPublishableArtifactLifecycleTask @Inject constructor(
        @get:Nested val extension: PlayPublisherExtension,
        executionDir: Directory,
) : DefaultTask(),
        GlobalPublishableArtifactExtensionOptions by CliOptionsImpl(extension, executionDir)

@DisableCachingByDefault
internal abstract class GlobalUploadableArtifactLifecycleTask @Inject constructor(
        @get:Nested val extension: PlayPublisherExtension,
        executionDir: Directory,
) : DefaultTask(), ArtifactExtensionOptions by CliOptionsImpl(extension, executionDir)

@DisableCachingByDefault
internal abstract class BootstrapLifecycleTask @Inject constructor(
        optionsHolder: BootstrapOptions.Holder,
) : DefaultTask(), BootstrapOptions by optionsHolder
