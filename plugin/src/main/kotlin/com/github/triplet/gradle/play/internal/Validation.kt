package com.github.triplet.gradle.play.internal

import com.android.Version
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

private val MIN_GRADLE_VERSION = GradleVersion.version("5.6")
private val MIN_AGP_VERSION = VersionNumber.parse("3.5.0-rc03")

internal fun validateRuntime() {
    val gradleVersion = GradleVersion.current()
    check(gradleVersion >= MIN_GRADLE_VERSION) {
        "Gradle Play Publisher's minimum Gradle version is at least $MIN_GRADLE_VERSION and " +
                "yours is $gradleVersion. Find the latest version at " +
                "https://github.com/gradle/gradle/releases, then run " +
                "'./gradlew wrapper --gradle-version=\$LATEST --distribution-type=ALL'."
    }

    val agpVersion = VersionNumber.parse(try {
        Version.ANDROID_GRADLE_PLUGIN_VERSION
    } catch (e: NoClassDefFoundError) {
        @Suppress("DEPRECATION") // TODO remove when 3.6 is the minimum
        com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
    })
    check(agpVersion >= MIN_AGP_VERSION) {
        "Gradle Play Publisher's minimum Android Gradle Plugin version is at least " +
                "$MIN_AGP_VERSION and yours is $agpVersion. Find the latest version and upgrade " +
                "instructions at https://developer.android.com/studio/releases/gradle-plugin."
    }
    // TODO remove when 3.6 is the minimum
    check(agpVersion < VersionNumber.parse("3.6.0-alpha01") ||
                  agpVersion >= VersionNumber.parse("3.6.0-alpha05")
    ) { "GPP is only known to be compatible with AGP 3.6 alpha 5. Please upgrade." }
}

internal infix fun GoogleJsonResponseException.has(error: String) =
        details?.errors.orEmpty().any { it.reason == error }
