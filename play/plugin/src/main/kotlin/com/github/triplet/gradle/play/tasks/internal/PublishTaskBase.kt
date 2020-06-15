package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.tasks.Input

internal abstract class PublishTaskBase(
        extension: PlayPublisherExtension,
        @get:Input internal val appId: String
) : PlayTaskBase(extension)
