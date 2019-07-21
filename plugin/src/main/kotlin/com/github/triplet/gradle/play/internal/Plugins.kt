package com.github.triplet.gradle.play.internal

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.ProductFlavor
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.CommitEdit
import com.github.triplet.gradle.play.tasks.GenerateEdit
import com.github.triplet.gradle.play.tasks.internal.EditTaskBase
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import java.io.File

internal val BaseVariant.flavorNameOrDefault get() = flavorName.nullOrFull() ?: "main"

internal val BaseVariant.playPath get() = "$RESOURCES_OUTPUT_PATH/$name"

private val ProductFlavor.extras
    get() = requireNotNull((this as ExtensionAware).the<ExtraPropertiesExtension>())

internal operator fun ProductFlavor.get(name: String) = extras[name]

internal operator fun ProductFlavor.set(name: String, value: Any?) {
    extras[name] = value
}

internal fun Project.newTask(
        name: String,
        description: String? = null,
        block: Task.() -> Unit = {}
) = newTask(name, description, emptyArray(), block)

internal inline fun <reified T : Task> Project.newTask(
        name: String,
        description: String? = null,
        constructorArgs: Array<Any> = emptyArray(),
        noinline block: T.() -> Unit = {}
): TaskProvider<T> {
    val config: T.() -> Unit = {
        this.description = description
        this.group = PLUGIN_GROUP.takeUnless { description.isNullOrBlank() }
        block()
    }

    val safeName = if (tasks.findByName(name) == null) name else "gpp" + name.capitalize()
    return tasks.register<T>(safeName, *constructorArgs).apply { configure(config) }
}

internal fun Project.getGenEditTask(
        appId: String,
        extension: PlayPublisherExtension
) = rootProject.getOrRegisterEditTask<GenerateEdit>("generateEditFor", extension, appId)

internal fun Project.getCommitEditTask(
        appId: String,
        extension: PlayPublisherExtension
) = rootProject.getOrRegisterEditTask<CommitEdit>("commitEditFor", extension, appId)

private inline fun <reified T : EditTaskBase> Project.getOrRegisterEditTask(
        baseName: String,
        extension: PlayPublisherExtension,
        appId: String
): TaskProvider<T> {
    val taskName = baseName + appId.split(".").joinToString("Dot") { it.capitalize() }
    return try {
        tasks.register<T>(taskName, extension).apply {
            configure {
                editIdFile.set(File(buildDir, "$OUTPUT_PATH/$appId.txt"))
            }
        }
    } catch (e: InvalidUserDataException) {
        @Suppress("UNCHECKED_CAST")
        tasks.named(taskName) as TaskProvider<T>
    }
}
