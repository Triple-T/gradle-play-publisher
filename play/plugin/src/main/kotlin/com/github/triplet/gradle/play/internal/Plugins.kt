package com.github.triplet.gradle.play.internal

import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.ApplicationVariantProperties
import com.github.triplet.gradle.common.utils.PLUGIN_GROUP
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.CommitEdit
import com.github.triplet.gradle.play.tasks.internal.PlayApiService
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import java.util.UUID

internal val ApplicationVariantProperties.flavorNameOrDefault
    get() = flavorName.nullOrFull() ?: "main"

internal val ApplicationVariantProperties.playPath get() = "$RESOURCES_OUTPUT_PATH/$name/$PLAY_PATH"

internal fun Project.newTask(
        name: String,
        description: String? = null,
        block: Task.() -> Unit = {},
) = newTask(name, description, emptyArray(), block)

internal inline fun <reified T : Task> Project.newTask(
        name: String,
        description: String? = null,
        constructorArgs: Array<Any> = emptyArray(),
        noinline block: T.() -> Unit = {},
): TaskProvider<T> {
    val config: T.() -> Unit = {
        this.description = description
        this.group = PLUGIN_GROUP.takeUnless { description.isNullOrBlank() }
        block()
    }

    return tasks.register<T>(name, *constructorArgs).apply { configure(config) }
}

internal fun Project.getCommitEditTask(
        appId: String,
        extension: PlayPublisherExtension,
        api: Provider<PlayApiService>,
): TaskProvider<CommitEdit> {
    val taskName = "commitEditFor" + appId.split(".").joinToString("Dot") { it.capitalize() }
    return try {
        rootProject.tasks.register<CommitEdit>(taskName, extension).apply {
            configure {
                apiService.set(api)
            }
        }
    } catch (e: InvalidUserDataException) {
        @Suppress("UNCHECKED_CAST")
        rootProject.tasks.named(taskName) as TaskProvider<CommitEdit>
    }
}

internal fun ApplicationVariant<ApplicationVariantProperties>.buildExtension(
        project: Project,
        extensionContainer: NamedDomainObjectContainer<PlayPublisherExtension>,
        baseExtension: PlayPublisherExtension,
        cliOptionsExtension: PlayPublisherExtension,
): PlayPublisherExtension = buildExtensionInternal(
        project,
        this,
        extensionContainer,
        baseExtension,
        cliOptionsExtension
)

private fun buildExtensionInternal(
        project: Project,
        variant: ApplicationVariant<ApplicationVariantProperties>,
        extensionContainer: NamedDomainObjectContainer<PlayPublisherExtension>,
        baseExtension: PlayPublisherExtension,
        cliOptionsExtension: PlayPublisherExtension,
): PlayPublisherExtension {
    val variantExtension = extensionContainer.findByName(variant.name)
    val flavorExtension = variant.productFlavors.mapNotNull { (_, flavor) ->
        extensionContainer.findByName(flavor)
    }.singleOrNull()
    val dimensionExtension = variant.productFlavors.mapNotNull { (dimension, _) ->
        extensionContainer.findByName(dimension)
    }.singleOrNull()
    val buildTypeExtension = variant.buildType?.let { extensionContainer.findByName(it) }

    val rawExtensions = listOf(
            cliOptionsExtension,
            variantExtension,
            flavorExtension,
            dimensionExtension,
            buildTypeExtension,
            baseExtension
    )
    val extensions = rawExtensions.filterNotNull().distinctBy {
        it.name
    }.map {
        val priority = rawExtensions.subList(1, rawExtensions.size).indexOfFirst { it != null }
        ExtensionMergeHolder(
                original = it,
                uninitializedCopy = project.objects.newInstance("$priority:${UUID.randomUUID()}")
        )
    }

    return mergeExtensions(extensions)
}

internal fun PlayPublisherExtension.toPriority() = name.split(":").first().toInt()
