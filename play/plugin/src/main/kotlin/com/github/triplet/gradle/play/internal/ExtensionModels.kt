package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.google.api.services.androidpublisher.model.TrackRelease

internal fun ReleaseStatus.isRollout() =
        this == ReleaseStatus.IN_PROGRESS || this == ReleaseStatus.HALTED

internal fun TrackRelease.isRollout() =
        status == ReleaseStatus.IN_PROGRESS.publishedName ||
                status == ReleaseStatus.HALTED.publishedName
