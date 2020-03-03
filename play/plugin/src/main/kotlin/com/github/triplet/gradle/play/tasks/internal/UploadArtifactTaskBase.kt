package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.config
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

internal abstract class UploadArtifactTaskBase(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishArtifactTaskBase(extension, variant) {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputFile
    internal val mappingFile: File?
        get() {
            val customDir = extension.config.artifactDir

            return if (customDir == null) {
                variant.mappingFileProvider.get().singleOrNull()?.orNull()
            } else {
                customDir.listFiles().orEmpty().singleOrNull { it.name == "mapping.txt" }
            }
        }
}
