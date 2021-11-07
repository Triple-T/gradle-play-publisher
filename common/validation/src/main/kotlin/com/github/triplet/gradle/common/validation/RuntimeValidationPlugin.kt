package com.github.triplet.gradle.common.validation

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

internal class RuntimeValidationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        check(project === project.rootProject)

        GradleRuntimeValidator(GradleVersion.current(), MIN_GRADLE_VERSION)
                .validate()
    }
}
