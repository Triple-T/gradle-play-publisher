package com.github.triplet.gradle.play.internal

import com.android.builder.model.Version
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import org.gradle.util.GradleVersion

private val MIN_GRADLE_VERSION: GradleVersion = GradleVersion.version("4.4")
private const val MIN_AGP_VERSION: String = "3.1.0"

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

internal fun PlayPublisherExtension.areCredsValid(): Boolean {
    val creds = _serviceAccountCredentials ?: return false
    return if (creds.extension.equals("json", true)) {
        serviceAccountEmail == null
    } else {
        serviceAccountEmail != null
    }
}

internal infix fun GoogleJsonResponseException.has(error: String) =
        details?.errors.orEmpty().any { it.reason == error }
