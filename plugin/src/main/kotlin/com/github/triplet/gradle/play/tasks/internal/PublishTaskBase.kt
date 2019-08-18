package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.tasks.Internal

abstract class PublishTaskBase(
        extension: PlayPublisherExtension,
        @get:Internal internal val variant: ApplicationVariant
) : PlayTaskBase(extension)
