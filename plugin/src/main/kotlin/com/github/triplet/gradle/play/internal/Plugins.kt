package com.github.triplet.gradle.play.internal

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.ProductFlavor
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.the

internal val BaseVariant.flavorNameOrDefault get() = flavorName.nullOrFull() ?: "main"

internal val BaseVariant.playPath get() = "$RESOURCES_OUTPUT_PATH/$name"

private val ProductFlavor.extras
    get() = requireNotNull((this as ExtensionAware).the<ExtraPropertiesExtension>())

internal inline fun <reified T : Task> Project.newTask(
        name: String,
        description: String,
        group: String? = PLUGIN_GROUP,
        crossinline block: T.() -> Unit = {}
) = task<T>(name) {
    this.description = description
    this.group = group
    block()
}

internal operator fun ProductFlavor.get(name: String) = extras[name]

internal operator fun ProductFlavor.set(name: String, value: Any?) {
    extras[name] = value
}
