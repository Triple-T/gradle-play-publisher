package de.triplet.gradle.play.internal

internal val TrackType.superiors get() = TrackType.values().takeWhile { it != this && it != TrackType.ROLLOUT }

internal enum class TrackType(val publishedName: String) {
    // Note: changing the order breaks API compatibility
    INTERNAL("internal"),
    ALPHA("alpha"),
    BETA("beta"),
    ROLLOUT("rollout"),
    PRODUCTION("production");

    companion object {
        fun fromString(value: String): TrackType {
            return values().first { it.publishedName.equals(value, true) }
        }
    }
}

internal enum class ReleaseStatus(val status: String) {
    COMPLETED("completed"),
    DRAFT("draft"),
    HALTED("halted"),
    INPROGRESS("inProgress");
}

