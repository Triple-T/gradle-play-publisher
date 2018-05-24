package de.triplet.gradle.play.test

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging

class AndroidDefaultConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val android = project.extensions.getByType(AppExtension::class.java)
                ?: throw IllegalStateException("The 'com.android.application' plugin is required.")
        Logging.getLogger(AndroidDefaultConfigPlugin::class.java)
                .log(LogLevel.INFO, "${android.defaultConfig}")
    }
}
