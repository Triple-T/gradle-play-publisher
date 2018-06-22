package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import javax.inject.Inject

open class LifecycleHelperTask @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension
) : DefaultTask(), ExtensionOptions
