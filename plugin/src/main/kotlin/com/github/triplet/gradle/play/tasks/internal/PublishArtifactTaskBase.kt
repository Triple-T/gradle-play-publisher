package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import com.github.triplet.gradle.play.internal.orNull
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

abstract class PublishArtifactTaskBase(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant,
        optionsHolder: TransientTrackOptions.Holder
) : PublishEditTaskBase(extension, variant), TransientTrackOptions by optionsHolder {
    @get:Internal internal abstract val resDir: DirectoryProperty

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputDirectory
    internal val releaseNotesDir
        get() = resDir.file(RELEASE_NOTES_PATH).get().asFile.orNull()

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputDirectory
    internal val consoleNamesDir
        get() = resDir.file(RELEASE_NAMES_PATH).get().asFile.orNull()

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputFile
    internal val mappingFile: File?
        get() {
            val customDir = extension.config.artifactDir

            return if (customDir == null) {
                variant.mappingFileProvider.get().singleOrNull()
            } else {
                customDir.listFiles().orEmpty().singleOrNull { it.name == "mapping.txt" }
            }
        }
}
