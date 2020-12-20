package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

internal abstract class PublishTaskBase(
        extension: PlayPublisherExtension
) : PlayTaskBase(extension) {
    @get:Internal
    abstract val apiService: Property<PlayApiService>
}
