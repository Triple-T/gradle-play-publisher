package de.triplet.gradle.play.internal

val Track.publishedName get() = name.toLowerCase()
val Track.superiors get() = Track.values().takeWhile { it != this && it != Track.ROLLOUT }

enum class Track {
    INTERNAL,
    ALPHA,
    BETA,
    ROLLOUT,
    PRODUCTION;
}
