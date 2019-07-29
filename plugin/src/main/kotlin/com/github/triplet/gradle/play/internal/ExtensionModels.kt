package com.github.triplet.gradle.play.internal

import com.google.api.services.androidpublisher.model.TrackRelease

internal fun ReleaseStatus.isRollout() =
        this == ReleaseStatus.IN_PROGRESS || this == ReleaseStatus.HALTED

internal fun TrackRelease.isRollout() =
        status == ReleaseStatus.IN_PROGRESS.publishedName ||
                status == ReleaseStatus.HALTED.publishedName

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
