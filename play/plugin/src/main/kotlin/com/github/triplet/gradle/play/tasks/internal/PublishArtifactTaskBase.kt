package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

internal abstract class PublishArtifactTaskBase(
        extension: PlayPublisherExtension,
        appId: String
) : PublishEditTaskBase(extension, appId) {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    internal abstract val releaseNotesDir: DirectoryProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    internal abstract val consoleNamesDir: DirectoryProperty
}
