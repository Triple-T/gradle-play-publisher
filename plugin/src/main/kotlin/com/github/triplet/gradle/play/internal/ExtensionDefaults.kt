package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.play.PlayPublisherExtension

internal val PlayPublisherExtension.trackOrDefault get() = _track ?: TrackType.INTERNAL
internal val PlayPublisherExtension.releaseStatusOrDefault
    get() = _releaseStatus ?: ReleaseStatus.COMPLETED
internal val PlayPublisherExtension.resolutionStrategyOrDefault
    get() = _resolutionStrategy ?: ResolutionStrategy.FAIL

internal fun PlayPublisherExtension?.mergeWith(
        default: PlayPublisherExtension
): PlayPublisherExtension {
    if (this == null) return default

    for (field in PlayPublisherExtension::class.java.declaredFields) {
        if (field.name.startsWith("_")) { // Backing field
            field.isAccessible = true
            if (field[this] == null) field[this] = field[default]
        }
    }

    return this
}
