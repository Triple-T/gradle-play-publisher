package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

abstract class PlayPublishTaskBase(
        @get:Nested internal open val extension: PlayPublisherExtension,
        @get:Internal internal val variant: ApplicationVariant
) : DefaultTask()
