package de.triplet.gradle.play.test


import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging

class AndroidDefaultConfigPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (!project.plugins.any { p -> p instanceof AppPlugin }) {
            throw new IllegalStateException('The \'com.android.application\' plugin is required.')
        }
        def android = project.extensions.getByType(AppExtension)
        Logging.getLogger(AndroidDefaultConfigPlugin)
                .log(LogLevel.INFO, "${android.defaultConfig}")
    }
}
