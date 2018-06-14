package com.github.triplet.gradle.play.internal

internal enum class TrackType(val publishedName: String) {
    // Note: changing the order breaks API compatibility
    INTERNAL("internal"),
    ALPHA("alpha"),
    BETA("beta"),
    ROLLOUT("rollout"),
    PRODUCTION("production")
}

internal enum class ReleaseStatus(val publishedName: String) {
    COMPLETED("completed"),
    DRAFT("draft"),
    HALTED("halted"),
    IN_PROGRESS("inProgress");

    companion object {
        fun fromString(value: String) = values().first { it.publishedName.equals(value, true) }
    }
}
