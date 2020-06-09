package com.github.triplet.gradle.play.internal

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.Action
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.Serializable
import kotlin.reflect.KMutableProperty1

internal val PlayExtensionConfig.serviceAccountCredentialsOrDefault: InputStream
    get() {
        return serviceAccountCredentials?.inputStream() ?: ByteArrayInputStream(
                System.getenv(PlayPublisher.CREDENTIAL_ENV_VAR).toByteArray())
    }
internal val PlayExtensionConfig.commitOrDefault get() = commit ?: true
internal val PlayExtensionConfig.trackOrDefault get() = track ?: "internal"
internal val PlayExtensionConfig.promoteTrackOrDefault
    get() = promoteTrack ?: trackOrDefault
internal val PlayExtensionConfig.releaseStatusOrDefault
    get() = releaseStatus ?: ReleaseStatus.COMPLETED
internal val PlayExtensionConfig.userFractionOrDefault
    get() = userFraction ?: 0.1
internal val PlayExtensionConfig.resolutionStrategyOrDefault
    get() = resolutionStrategy ?: ResolutionStrategy.FAIL

internal val PlayPublisherExtension.config
    get() = _config.copy()
internal val PlayPublisherExtension.serializableConfig
    get() = _config.copy(outputProcessor = null)

internal fun textToResolutionStrategy(input: String): ResolutionStrategy {
    return requireNotNull(
            ResolutionStrategy.values().find { it.publishedName == input }
    ) {
        "Resolution strategy must be one of " +
                ResolutionStrategy.values().joinToString { "'${it.publishedName}'" }
    }
}

internal fun textToReleaseStatus(input: String): ReleaseStatus {
    return requireNotNull(
            ReleaseStatus.values().find { it.publishedName == input }
    ) {
        "Release Status must be one of " +
                ReleaseStatus.values().joinToString { "'${it.publishedName}'" }
    }
}

internal fun <T> PlayPublisherExtension.updateProperty(
        property: KMutableProperty1<PlayExtensionConfig, T>,
        value: T,
        force: Boolean = false
) {
    for (callback in _callbacks) {
        @Suppress("UNCHECKED_CAST")
        callback(property as KMutableProperty1<PlayExtensionConfig, Any?>, value)
    }

    for (child in _children) {
        if (force || property.get(child._config) == null) {
            child.updateProperty(property, value, force)
        }
    }
}

internal fun PlayPublisherExtension.evaluate() {
    for (child in _children) {
        child.mergeWith(this)
        child.evaluate()
    }
}

internal fun mergeExtensions(extensions: List<PlayPublisherExtension>): PlayPublisherExtension {
    requireNotNull(extensions.isNotEmpty()) { "At least one extension must be provided." }
    if (extensions.size == 1) return extensions.single()

    for (i in 1 until extensions.size) {
        extensions[i]._children += extensions[i - 1]
    }

    return extensions.first()
}

internal fun PlayPublisherExtension.mergeWith(
        default: PlayPublisherExtension?
): PlayPublisherExtension {
    if (default == null) return this

    val baseConfig = default._config
    val mergeableConfig = _config
    for (field in PlayExtensionConfig::class.java.declaredFields) {
        field.isAccessible = true
        if (field[mergeableConfig] == null) {
            field[mergeableConfig] = field[baseConfig]
        }
    }

    return this
}

internal data class PlayExtensionConfig(
        var enabled: Boolean? = null,
        var serviceAccountCredentials: File? = null,
        var defaultToAppBundles: Boolean? = null,
        var commit: Boolean? = null,
        var fromTrack: String? = null,
        var track: String? = null,
        var promoteTrack: String? = null,
        var userFraction: Double? = null,
        var updatePriority: Int? = null,
        var resolutionStrategy: ResolutionStrategy? = null,
        var outputProcessor: Action<ApkVariantOutput>? = null,
        var releaseStatus: ReleaseStatus? = null,
        var releaseName: String? = null,
        var artifactDir: File? = null,
        var retainArtifacts: List<Long>? = null,
        var retainMainObb: Int? = null,
        var retainPatchObb: Int? = null
) : Serializable
