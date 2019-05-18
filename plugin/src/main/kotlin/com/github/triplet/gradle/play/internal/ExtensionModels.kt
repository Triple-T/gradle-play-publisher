package com.github.triplet.gradle.play.internal

internal enum class ReleaseStatus(val publishedName: String) {
    COMPLETED("completed"),
    DRAFT("draft"),
    HALTED("halted"),
    IN_PROGRESS("inProgress")
}

internal enum class ResolutionStrategy(val publishedName: String) {
    AUTO("auto"),
    FAIL("fail"),
    IGNORE("ignore")
}
