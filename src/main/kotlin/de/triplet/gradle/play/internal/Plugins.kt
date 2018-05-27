package de.triplet.gradle.play.internal

import com.android.builder.model.ProductFlavor
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension

private val ProductFlavor.extras
    get() = requireNotNull((this as ExtensionAware).extensions.get<ExtraPropertiesExtension>())

inline fun <reified T> ExtensionContainer.get() = findByType(T::class.java)

inline fun <reified T : Task> Project.newTask(
        name: String,
        description: String,
        group: String = PLUGIN_GROUP,
        block: T.() -> Unit = {}
): T = tasks.create(name, T::class.java).apply {
    this.description = description
    this.group = group
    block()
}

operator fun ProductFlavor.get(name: String) = extras[name]

operator fun ProductFlavor.set(name: String, value: Any?) {
    extras[name] = value
}
