package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested

internal abstract class PlayTaskBase(
        @get:Nested val extension: PlayPublisherExtension
) : DefaultTask()
