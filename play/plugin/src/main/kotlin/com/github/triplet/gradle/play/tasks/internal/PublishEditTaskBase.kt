package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState

internal abstract class PublishEditTaskBase(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishTaskBase(extension, variant) {
    @get:LocalState
    abstract val editIdFile: RegularFileProperty
    @get:Internal
    val editId by lazy { editIdFile.get().asFile.readText() }
}
