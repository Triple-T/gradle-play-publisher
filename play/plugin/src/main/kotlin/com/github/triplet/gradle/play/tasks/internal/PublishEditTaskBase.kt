package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal

internal abstract class PublishEditTaskBase(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishTaskBase(extension, variant) {
    @get:Internal
    abstract val editIdFile: RegularFileProperty

    @get:Internal
    val editId by lazy { editIdFile.get().asFile.readText() }
}
