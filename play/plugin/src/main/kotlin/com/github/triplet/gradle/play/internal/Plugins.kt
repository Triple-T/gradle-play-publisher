package com.github.triplet.gradle.play.internal

import com.android.build.api.variant.ApplicationVariant
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

internal val ApplicationVariant.flavorNameOrDefault
    get() = flavorName.nullOrFull() ?: "main"

internal val ApplicationVariant.playPath get() = "$RESOURCES_OUTPUT_PATH/$name/$PLAY_PATH"

internal inline fun <reified T : Task> Project.newTask(
        name: String,
        description: String? = null,
        constructorArgs: Array<Any> = emptyArray(),
        allowExisting: Boolean = false,
        noinline block: T.() -> Unit = {},
): TaskProvider<T> {
    val config: T.() -> Unit = {
        this.description = description
        this.group = PLUGIN_GROUP.takeUnless { description.isNullOrBlank() }
        block()
    }

    return try {
        tasks.register<T>(name, *constructorArgs).apply { configure(config) }
    } catch (e: InvalidUserDataException) {
        if (allowExisting) {
            @Suppress("UNCHECKED_CAST")
            tasks.named(name) as TaskProvider<T>
        } else {
            throw e
        }
    }
}

internal fun Project.getCommitEditTask(
        appId: String,
        extension: PlayPublisherExtension,
        api: Provider<PlayApiService>,
): TaskProvider<CommitEdit> {
    val taskName = "commitEditFor" + appId.split(".").joinToString("Dot") { it.capitalize() }
    return rootProject.newTask(taskName, allowExisting = true, constructorArgs = arrayOf(extension)) {
        usesService(api)
        apiService.set(api)
        onlyIf { !api.get().buildFailed }
    }
}

internal fun ApplicationVariant.buildExtension(
        project: Project,
        extensionContainer: NamedDomainObjectContainer<PlayPublisherExtension>,
        baseExtension: PlayPublisherExtension,
        cliOptionsExtension: PlayPublisherExtension,
): Map<String, PlayPublisherExtension> = buildExtensionInternal(
        project,
        this,
        extensionContainer,
        baseExtension,
        cliOptionsExtension
)

internal fun ApplicationVariant.generateExtensionOverrideOrdering(): List<String> = listOfNotNull(
        "__CLI__",
        name,
        *productFlavors.map { (_, flavor) -> flavor }.toTypedArray(),
        *productFlavors.map { (dimension, _) -> dimension }.toTypedArray(),
        buildType,
        "__ROOT__",
)

private fun buildExtensionInternal(
        project: Project,
        variant: ApplicationVariant,
        extensionContainer: NamedDomainObjectContainer<PlayPublisherExtension>,
        baseExtension: PlayPublisherExtension,
        cliOptionsExtension: PlayPublisherExtension,
): Map<String, PlayPublisherExtension> {
    val rawExtensions = variant.generateExtensionOverrideOrdering().map { name ->
        when (name) {
            "__CLI__" -> cliOptionsExtension
            "__ROOT__" -> baseExtension
            else -> extensionContainer.findByName(name)
        }?.let { name to it }
    }

    val priority = rawExtensions.subList(1, rawExtensions.size).indexOfFirst { it != null }
    val extensions = rawExtensions.filterNotNull().map { it.second }.distinctBy {
        it.name
    }.map { extension ->
        ExtensionMergeHolder(
                original = extension,
                uninitializedCopy = project.objects.newInstance("$priority:${UUID.randomUUID()}"),
        )
    }

    return mapOf(
            *rawExtensions.filterNotNull().toTypedArray(),
            variant.name to mergeExtensions(extensions),
    )
}

internal fun PlayPublisherExtension.toPriority() = name.split(":").first().toInt()
