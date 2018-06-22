package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import javax.inject.Inject

open class LifecycleHelperTask @Inject constructor(
        override val extension: PlayPublisherExtension
) : DefaultTask(), ExtensionOptions
