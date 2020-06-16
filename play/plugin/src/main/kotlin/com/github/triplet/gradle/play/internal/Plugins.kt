package com.github.triplet.gradle.play.internal

import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.ApplicationVariantProperties
import com.github.triplet.gradle.common.utils.PLUGIN_GROUP
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.CommitEdit
import com.github.triplet.gradle.play.tasks.GenerateEdit
import com.github.triplet.gradle.play.tasks.internal.EditTaskBase
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

internal val ApplicationVariantProperties.flavorNameOrDefault
    get() = flavorName.nullOrFull() ?: "main"

internal val ApplicationVariantProperties.playPath get() = "$RESOURCES_OUTPUT_PATH/$name/$PLAY_PATH"

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

    return tasks.register<T>(name, *constructorArgs).apply { configure(config) }
}

internal fun Project.getGenEditTask(
        appId: String,
        extension: PlayPublisherExtension
) = rootProject.getOrRegisterEditTask<GenerateEdit>("generateEditFor", extension, appId)

internal fun Project.getCommitEditTask(
        appId: String,
        extension: PlayPublisherExtension
) = rootProject.getOrRegisterEditTask<CommitEdit>("commitEditFor", extension, appId)

internal fun ApplicationVariant<ApplicationVariantProperties>.buildExtension(
        extensionContainer: NamedDomainObjectContainer<PlayPublisherExtension>,
        baseExtension: PlayPublisherExtension,
        cliOptionsExtension: PlayPublisherExtension
): PlayPublisherExtension = buildExtensionInternal(
        this,
        extensionContainer,
        baseExtension,
        cliOptionsExtension
)

private fun buildExtensionInternal(
        variant: ApplicationVariant<ApplicationVariantProperties>,
        extensionContainer: NamedDomainObjectContainer<PlayPublisherExtension>,
        baseExtension: PlayPublisherExtension,
        cliOptionsExtension: PlayPublisherExtension
): PlayPublisherExtension {
    val variantExtension = extensionContainer.findByName(variant.name)
    val flavorExtension = variant.productFlavors.mapNotNull { (_, flavor) ->
        extensionContainer.findByName(flavor)
    }.singleOrNull()
    val buildTypeExtension = variant.buildType?.let { extensionContainer.findByName(it) }

    val extensions = listOfNotNull(
            cliOptionsExtension,
            variantExtension,
            flavorExtension,
            buildTypeExtension,
            baseExtension
    ).distinctBy {
        it.name
    }

    return mergeExtensions(extensions)
}

private inline fun <reified T : EditTaskBase> Project.getOrRegisterEditTask(
        baseName: String,
        extension: PlayPublisherExtension,
        appId: String
): TaskProvider<T> {
    val taskName = baseName + appId.split(".").joinToString("Dot") { it.capitalize() }
    return try {
        tasks.register<T>(taskName, extension).apply {
            configure {
                editIdFile.set(layout.buildDirectory.file("$OUTPUT_PATH/$appId.txt"))
            }
        }
    } catch (e: InvalidUserDataException) {
        @Suppress("UNCHECKED_CAST")
        tasks.named(taskName) as TaskProvider<T>
    }
}
