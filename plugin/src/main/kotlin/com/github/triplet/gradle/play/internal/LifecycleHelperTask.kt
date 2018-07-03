package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested

internal open class LifecycleHelperTask : DefaultTask(), ExtensionOptions {
    @get:Nested override lateinit var extension: PlayPublisherExtension
}
