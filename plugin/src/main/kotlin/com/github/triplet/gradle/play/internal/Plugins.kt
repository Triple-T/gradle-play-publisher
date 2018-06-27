package com.github.triplet.gradle.play.internal

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.ProductFlavor
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension

internal val BaseVariant.flavorNameOrDefault get() = flavorName.nullOrFull() ?: "main"

internal val BaseVariant.playPath get() = "$RESOURCES_OUTPUT_PATH/$name"

private val ProductFlavor.extras
    get() = requireNotNull((this as ExtensionAware).extensions.get<ExtraPropertiesExtension>())

internal inline fun <reified T> ExtensionContainer.get() = findByType(T::class.java)

internal inline fun <reified T : Task> Project.newTask(
        name: String,
        description: String,
        group: String? = PLUGIN_GROUP,
        vararg args: Any?,
        block: T.() -> Unit = {}
): T = tasks.create(name, T::class.java, *args).apply {
    this.description = description
    this.group = group
    block()
}

internal operator fun ProductFlavor.get(name: String) = extras[name]

internal operator fun ProductFlavor.set(name: String, value: Any?) {
    extras[name] = value
}
