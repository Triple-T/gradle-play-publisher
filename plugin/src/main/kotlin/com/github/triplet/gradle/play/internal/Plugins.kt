package com.github.triplet.gradle.play.internal

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.ProductFlavor
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.the
import org.gradle.util.GradleVersion

internal val BaseVariant.flavorNameOrDefault get() = flavorName.nullOrFull() ?: "main"

internal val BaseVariant.playPath get() = "$RESOURCES_OUTPUT_PATH/$name"

private val ProductFlavor.extras
    get() = requireNotNull((this as ExtensionAware).the<ExtraPropertiesExtension>())

private val isConfigurationAvoidanceSupported =
        GradleVersion.current() >= GradleVersion.version("4.10")

internal inline fun <reified T : Task> Project.newTask(
        name: String,
        description: String,
        group: String? = PLUGIN_GROUP,
        crossinline block: T.() -> Unit = {}
): Provider<T> {
    val config: T.() -> Unit = {
        this.description = description
        this.group = group
        block()
    }

    return if (isConfigurationAvoidanceSupported) {
        tasks.register(name, config)
    } else {
        val task = task(name, config)
        DefaultProvider { task }
    }
}

internal fun <T : Task> Provider<T>.configure(block: T.() -> Unit) {
    if (isConfigurationAvoidanceSupported) {
        (this as TaskProvider<T>).configure(block)
    } else {
        get().apply(block)
    }
}

internal operator fun ProductFlavor.get(name: String) = extras[name]

internal operator fun ProductFlavor.set(name: String, value: Any?) {
    extras[name] = value
}
