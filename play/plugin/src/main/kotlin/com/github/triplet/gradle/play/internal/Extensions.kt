package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.File
import java.io.Serializable
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

internal fun PlayPublisherExtension.toConfig() = PlayExtensionConfig(
        enabled.get(),
        serviceAccountCredentials.asFile.orNull,
        defaultToAppBundles.get(),
        commit.get(),
        fromTrack.orNull,
        track.get(),
        promoteTrack.orNull,
        userFraction.orNull,
        updatePriority.orNull,
        releaseStatus.orNull,
        releaseName.orNull,
        versionCode.orNull,
        resolutionStrategy.get(),
        retain.artifacts.orNull,
        retain.mainObb.orNull,
        retain.patchObb.orNull
)

internal fun mergeExtensions(extensions: List<ExtensionMergeHolder>): PlayPublisherExtension {
    requireNotNull(extensions.isNotEmpty()) { "At least one extension must be provided." }
    if (extensions.size == 1) return extensions.single().original

    val extensionsWithRootInitialization = extensions + extensions.last()
    return mergeExtensionsInternal(extensionsWithRootInitialization)
}

/**
 * Support for cascading extension values is one of GPP's core features. We need to support a global
 * default value store for the root `play` extension, independent extensions for variants, flavors,
 * etc, and another global store for CLI args that overrides everything. Most of the complexity is
 * due to the CLI args store because values are assigned after tasks have been created, which means
 * we need a way to retro-actively update values set through other extensions.
 *
 * The current solution creates a tree of dependencies, where the leaf nodes represent the resolved
 * extension for a specific AGP variant, and the root node is the default store. In detail:
 *
 * - We get a list of extensions which represent a path up the tree, where the first value is the
 *   extension that will be used and subsequent values are linked to the first, providing defaults.
 * - As we go through each extension, we reflectively access all the extensions properties and link
 *   each one individually.
 * - To link a property, we hack our way around Gradle's API because they have terrible support for
 *   modelling defaults:
 *    - An uninitialized copy of the property is made (to support defaults).
 *    - The original property (potentially with a value present) is put into the copy with an orElse
 *      statement linking the original property to its parent node.
 *    - List properties are particularly painful because null gets mapped to empty by default so we
 *      have to undo that mapping.
 */
private fun mergeExtensionsInternal(
        extensions: List<ExtensionMergeHolder>,
): PlayPublisherExtension {
    for (i in 1 until extensions.size) {
        val parentCopy = extensions[i].uninitializedCopy
        val (child, childCopy) = extensions[i - 1]

        PlayPublisherExtension::class.declaredMemberProperties
                .linkProperties(parentCopy, child, childCopy)
        PlayPublisherExtension.Retain::class.declaredMemberProperties
                .linkProperties(parentCopy.retain, child.retain, childCopy.retain)
    }

    return extensions.first().uninitializedCopy
}

private fun <T> Collection<KProperty1<T, *>>.linkProperties(parent: T, child: T, childCopy: T) {
    for (property in this) {
        if (property.name == "name") continue

        val value = property.get(childCopy)
        @Suppress("UNCHECKED_CAST")
        if (value is Property<*>) {
            val originalProperty = property.get(child) as Property<Nothing>
            val parentFallback = property.get(parent) as Property<Nothing>
            if (value !== parentFallback) {
                value.set(originalProperty.orElse(parentFallback))
            } else {
                value.set(originalProperty)
            }
        } else if (value is ListProperty<*>) {
            val originalProperty = property.get(child) as ListProperty<Nothing>
            val parentFallback = property.get(parent) as ListProperty<Nothing>
            if (value !== parentFallback) {
                value.set(originalProperty.map {
                    it.takeUnless { it.isEmpty() }.sneakyNull()
                }.orElse(parentFallback))
            } else {
                value.set(originalProperty)
            }
        }
    }
}

// TODO(asaveau): remove after https://github.com/gradle/gradle/issues/12388
@Suppress("UNCHECKED_CAST")
private fun <T> T?.sneakyNull() = this as T

internal abstract class CliPlayPublisherExtension : PlayPublisherExtension("cliOptions")

internal data class PlayExtensionConfig(
        val enabled: Boolean,
        val serviceAccountCredentials: File?,
        val defaultToAppBundles: Boolean,
        val commit: Boolean,
        val fromTrack: String?,
        val track: String,
        val promoteTrack: String?,
        val userFraction: Double?,
        val updatePriority: Int?,
        val releaseStatus: ReleaseStatus?,
        val releaseName: String?,
        val versionCode: Long?,
        val resolutionStrategy: ResolutionStrategy,
        val retainArtifacts: List<Long>?,
        val retainMainObb: Int?,
        val retainPatchObb: Int?,
) : Serializable

internal data class ExtensionMergeHolder(
        val original: PlayPublisherExtension,
        val uninitializedCopy: PlayPublisherExtension,
)
