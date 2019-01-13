package com.github.triplet.gradle.play.internal

internal enum class ReleaseStatus(val publishedName: String) {
    COMPLETED("completed"),
    DRAFT("draft"),
    HALTED("halted"),
    IN_PROGRESS("inProgress")
}
