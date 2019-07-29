package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.play.PlayPublisherExtension

internal val PlayPublisherExtension.Config.commitOrDefault get() = commit ?: true
internal val PlayPublisherExtension.Config.trackOrDefault get() = track ?: "internal"
internal val PlayPublisherExtension.Config.promoteTrackOrDefault
    get() = promoteTrack ?: trackOrDefault
internal val PlayPublisherExtension.Config.releaseStatusOrDefault
    get() = releaseStatus ?: ReleaseStatus.COMPLETED
internal val PlayPublisherExtension.Config.userFractionOrDefault
    get() = userFraction ?: 0.1
internal val PlayPublisherExtension.Config.resolutionStrategyOrDefault
    get() = resolutionStrategy ?: ResolutionStrategy.FAIL

fun mergeExtensions(extensions: List<PlayPublisherExtension>): PlayPublisherExtension {
    requireNotNull(extensions.isNotEmpty()) { "At least one extension must be provided." }
    if (extensions.size == 1) return extensions.single()

    var result = extensions.first()
    for (i in 1 until extensions.size) {
        result = result.mergeWith(extensions[i])
    }
    return result
}

fun PlayPublisherExtension.mergeWith(default: PlayPublisherExtension?): PlayPublisherExtension {
    if (default == null) return this

    fun PlayPublisherExtension.getMutableConfig(): Any {
        val field = PlayPublisherExtension::class.java.getDeclaredField("_config")
        field.isAccessible = true
        return field[this]
    }

    val baseConfig = default.getMutableConfig()
    val mergeableConfig = getMutableConfig()
    for (field in PlayPublisherExtension.Config::class.java.declaredFields) {
        field.isAccessible = true
        if (field.name == "retain") {
            mergeRetain(field[mergeableConfig], field[baseConfig])
        } else if (field[mergeableConfig] == null) {
            field[mergeableConfig] = field[baseConfig]
        }
    }

    return this
}

private fun mergeRetain(mergeableRetain: Any, baseRetain: Any) {
    for (field in PlayPublisherExtension.Retain::class.java.declaredFields) {
        field.isAccessible = true
        if (field[mergeableRetain] == null) field[mergeableRetain] = field[baseRetain]
    }
}
