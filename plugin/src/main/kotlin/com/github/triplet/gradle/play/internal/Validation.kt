package com.github.triplet.gradle.play.internal

import com.android.builder.model.Version
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import org.gradle.util.GradleVersion

private val MIN_GRADLE_VERSION: GradleVersion = GradleVersion.version("4.1")
private const val MIN_AGP_VERSION: String = "3.0.1"

/** Release statuses that are compatible with a [PlayPublisherExtension.track] of `rollout` */
private val rolloutStatuses = listOf(ReleaseStatus.IN_PROGRESS, ReleaseStatus.HALTED)

internal fun validateRuntime() {
    val gradleVersion = GradleVersion.current()
    check(gradleVersion >= MIN_GRADLE_VERSION) {
        "Gradle Play Publisher's minimum Gradle version is at least $MIN_GRADLE_VERSION and " +
                "yours is $gradleVersion. Find the latest version at " +
                "https://github.com/gradle/gradle/releases, then run " +
                "'./gradlew wrapper --gradle-version=\$LATEST --distribution-type=ALL'."
    }

    val agpVersion = Version.ANDROID_GRADLE_PLUGIN_VERSION
    check(agpVersion >= MIN_AGP_VERSION) {
        "Gradle Play Publisher's minimum Android Gradle Plugin version is at least " +
                "$MIN_AGP_VERSION and yours is $agpVersion. Find the latest version and upgrade " +
                "instructions at https://developer.android.com/studio/releases/gradle-plugin."
    }
}

internal fun validatedTrack(value: String) = requireNotNull(
        TrackType.values().find { it.publishedName == value }
) { "Track must be one of ${TrackType.values().joinToString { "'${it.publishedName}'" }}" }

/**
 * Check the compatibility of [PlayPublisherExtension.track] and
 * [PlayPublisherExtension.releaseStatus].
 * For reference: [https://developers.google.com/android-publisher/api-ref/edits/tracks]
 */
internal fun PlayPublisherExtension.validate() {
    val usesRolloutStatues = rolloutStatuses.map { it.publishedName }.contains(releaseStatus)
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

internal infix fun GoogleJsonResponseException.has(error: String) =
        details?.errors.orEmpty().any { it.reason == error }
