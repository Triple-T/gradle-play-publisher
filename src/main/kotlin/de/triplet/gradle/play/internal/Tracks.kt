package de.triplet.gradle.play.internal

internal val Track.superiors get() = Track.values().takeWhile { it != this && it != Track.ROLLOUT }

internal enum class Track(val publishedName: String) {
    // Note: changing the order breaks API compatibility
    INTERNAL("internal"),
    ALPHA("alpha"),
    BETA("beta"),
    ROLLOUT("rollout"),
    PRODUCTION("production")
}
