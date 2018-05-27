package de.triplet.gradle.play.internal

internal val Track.publishedName get() = name.toLowerCase()
internal val Track.superiors get() = Track.values().takeWhile { it != this && it != Track.ROLLOUT }

internal enum class Track {
    INTERNAL,
    ALPHA,
    BETA,
    ROLLOUT,
    PRODUCTION;
}
