package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

internal abstract class PublishArtifactTaskBase(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishEditTaskBase(extension, variant) {
    @get:Internal
    internal abstract val resDir: DirectoryProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputDirectory
    internal val releaseNotesDir
        get() = resDir.dir(RELEASE_NOTES_PATH).optional()

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputDirectory
    internal val consoleNamesDir
        get() = resDir.dir(RELEASE_NAMES_PATH).optional()

    private fun Provider<Directory>.optional() =
            flatMap { project.objects.directoryProperty().apply { set(it.asFile.orNull()) } }
}
