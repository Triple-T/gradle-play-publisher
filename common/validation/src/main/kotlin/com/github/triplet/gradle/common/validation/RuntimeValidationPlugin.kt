package com.github.triplet.gradle.common.validation

import com.android.Version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

internal class RuntimeValidationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        check(project === project.rootProject)

        val agpVersion = try {
            VersionNumber.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION)
        } catch (e: NoClassDefFoundError) {
            null
        }
        val validator = RuntimeValidator(
                GradleVersion.current(), MIN_GRADLE_VERSION, agpVersion, MIN_AGP_VERSION)

        validator.validate()
    }

    private companion object {
        val MIN_GRADLE_VERSION = GradleVersion.version("6.5")
        val MIN_AGP_VERSION = VersionNumber.parse("4.1.0-beta01")
    }
}
