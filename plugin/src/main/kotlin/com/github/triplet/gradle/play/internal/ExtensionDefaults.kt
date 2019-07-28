package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.play.PlayPublisherExtension

internal val PlayPublisherExtension.Config.commitOrDefault get() = commit ?: true
internal val PlayPublisherExtension.Config.trackOrDefault get() = track ?: "internal"
internal val PlayPublisherExtension.Config.releaseStatusOrDefault
    get() = releaseStatus ?: ReleaseStatus.COMPLETED
internal val PlayPublisherExtension.Config.resolutionStrategyOrDefault
    get() = resolutionStrategy ?: ResolutionStrategy.FAIL

internal fun PlayPublisherExtension?.mergeWith(
        default: PlayPublisherExtension
): PlayPublisherExtension {
    if (this == null) return default

    fun PlayPublisherExtension.getMutableConfig(): Any {
        val field = PlayPublisherExtension::class.java.getDeclaredField("_config")
        field.isAccessible = true
        return field[this]
    }

    val baseConfig = default.getMutableConfig()
    val mergableConfig = getMutableConfig()
    for (field in PlayPublisherExtension.Config::class.java.declaredFields) {
        field.isAccessible = true
        if (field[mergableConfig] == null) field[mergableConfig] = field[baseConfig]
    }

    return this
}
