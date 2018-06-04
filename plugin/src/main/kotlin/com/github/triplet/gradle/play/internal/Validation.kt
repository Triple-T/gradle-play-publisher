package com.github.triplet.gradle.play.internal

import com.github.triplet.gradle.play.PlayPublisherExtension

/** Release statuses that are compatible with a [PlayPublisherExtension.track] of `rollout` */
private val rolloutStatuses = listOf(ReleaseStatus.IN_PROGRESS, ReleaseStatus.HALTED)

/**
 * Check the compatibility of [PlayPublisherExtension.track] and
 * [PlayPublisherExtension.releaseStatus].
 * For reference: [https://developers.google.com/android-publisher/api-ref/edits/tracks]
 */
internal fun PlayPublisherExtension.validate() {
    _releaseStatus = ReleaseStatus.fromString(releaseStatus)

    val usesRolloutStatues = rolloutStatuses.contains(_releaseStatus)
    if (_track == TrackType.ROLLOUT) {
        check(usesRolloutStatues) {
            "'rollout' track must use the 'inProgress' or 'halted' statues."
        }
    } else {
        check(!usesRolloutStatues) {
            "Track must be of type 'rollout' to use 'inProgress' or 'halted' statues."
        }
    }
}
