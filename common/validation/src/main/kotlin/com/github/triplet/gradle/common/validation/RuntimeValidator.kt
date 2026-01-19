package com.github.triplet.gradle.common.validation

import com.android.build.api.AndroidPluginVersion
import org.gradle.util.GradleVersion

internal class GradleRuntimeValidator(
        private val currentGradleVersion: GradleVersion,
        private val minGradleVersion: GradleVersion,
) {
    fun validate() {
        check(currentGradleVersion >= minGradleVersion) {
            $$"""
            |Gradle Play Publisher's minimum Gradle version is at least $$minGradleVersion and yours
            |is $$currentGradleVersion. Find the latest version at
            |https://github.com/gradle/gradle/releases/latest, then run
            |$ ./gradlew wrapper --gradle-version=$LATEST --distribution-type=ALL
            """.trimMargin()
        }
    }
}

internal class AgpRuntimeValidator(
        private val currentAgpVersion: AndroidPluginVersion?,
        private val minAgpVersion: AndroidPluginVersion,
) {
    fun validate() {
        check(null != currentAgpVersion && currentAgpVersion >= minAgpVersion) {
            """
            |Gradle Play Publisher's minimum Android Gradle Plugin version is at least
            |$minAgpVersion and yours is ${currentAgpVersion ?: "unknown"}. Make sure you've applied
            |the AGP alongside this plugin. Find the latest AGP version and upgrade
            |instructions at https://developer.android.com/studio/releases/gradle-plugin.
            |For GPP installation docs, see here:
            |https://github.com/Triple-T/gradle-play-publisher#installation
            """.trimMargin()
        }
    }
}
