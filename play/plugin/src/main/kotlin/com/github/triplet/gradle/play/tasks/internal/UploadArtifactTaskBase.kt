package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

internal abstract class UploadArtifactTaskBase(
        extension: PlayPublisherExtension,
        appId: String
) : PublishArtifactTaskBase(extension, appId) {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputFile
    internal abstract val mappingFile: RegularFileProperty
}
