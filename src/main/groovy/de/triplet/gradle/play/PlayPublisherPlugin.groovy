package de.triplet.gradle.play

import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class PlayPublisherPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def log = project.logger

        def hasAppPlugin = project.plugins.find { p -> p instanceof AppPlugin }

        if (!hasAppPlugin) {
            throw new IllegalStateException("The 'com.android.application' plugin is required.")
        }

        def extension = project.extensions.create('play', PlayPublisherPluginExtension)

        project.android.applicationVariants.all { variant ->
            def buildTypeName = variant.buildType.name.capitalize()

            if (!variant.buildType.name.equals("release")) {
                log.debug("Skipping build type ${variant.buildType.name}.")
                return;
            }

            def projectFlavorNames = variant.productFlavors.collect { it.name.capitalize() }

            if (projectFlavorNames.isEmpty()) {
                projectFlavorNames = [""]
            }

            def projectFlavorName = projectFlavorNames.join()

            def publishTaskName = "publish$projectFlavorName$buildTypeName"
            project.tasks.create(publishTaskName)
        }
    }

}
