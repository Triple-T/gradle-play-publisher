package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal

internal abstract class PublishEditTaskBase(
        extension: PlayPublisherExtension,
        appId: String
) : PublishTaskBase(extension, appId) {
    @get:Internal
    abstract val editIdFile: RegularFileProperty

    @get:Internal
    val editId by lazy { editIdFile.get().asFile.readText() }
}
