package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.Serializable
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

internal fun PlayPublisherExtension.toConfig() = PlayExtensionConfig(
        enabled.get(),
        serviceAccountCredentials.orNull?.asFile,
        defaultToAppBundles.get(),
        commit.get(),
        fromTrack.orNull,
        track.get(),
        promoteTrack.orNull,
        userFraction.orNull,
        updatePriority.orNull,
        releaseStatus.orNull,
        releaseName.orNull,
        resolutionStrategy.get(),
        retain.artifacts.orNull,
        retain.mainObb.orNull,
        retain.patchObb.orNull
)

internal fun PlayExtensionConfig.credentialStream(): InputStream {
    return serviceAccountCredentials?.inputStream() ?: ByteArrayInputStream(
            System.getenv(PlayPublisher.CREDENTIAL_ENV_VAR).toByteArray())
}

internal fun mergeExtensions(extensions: List<PlayPublisherExtension>): PlayPublisherExtension {
    requireNotNull(extensions.isNotEmpty()) { "At least one extension must be provided." }
    if (extensions.size == 1) return extensions.single()

    for (i in 1 until extensions.size) {
        val parent = extensions[i]
        val child = extensions[i - 1]

        PlayPublisherExtension::class.declaredMemberProperties.linkProperties(parent, child)
        PlayPublisherExtension.Retain::class.declaredMemberProperties
                .linkProperties(parent.retain, child.retain)
    }

    return extensions.first()
}

private fun <T> Collection<KProperty1<T, *>>.linkProperties(parent: T, child: T) {
    for (property in this) {
        if (property.name == "name") continue

        val value = property.get(child)
        @Suppress("UNCHECKED_CAST")
        if (value is Property<*>) {
            value.convention(property.get(parent) as Property<Nothing>)
        } else if (value is ListProperty<*>) {
            value.convention(property.get(parent) as ListProperty<Nothing>)
        }
    }
}

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
        val resolutionStrategy: ResolutionStrategy,
        val retainArtifacts: List<Long>?,
        val retainMainObb: Int?,
        val retainPatchObb: Int?
) : Serializable
