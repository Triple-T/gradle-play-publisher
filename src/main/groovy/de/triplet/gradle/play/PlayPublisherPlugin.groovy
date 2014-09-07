package de.triplet.gradle.play

import com.android.build.gradle.AppPlugin
import org.apache.commons.lang.StringUtils
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
            def zipalignTaskName = "zipalign$projectFlavorName$buildTypeName"
            def manifestTaskName = "process${projectFlavorName}${buildTypeName}Manifest"
            def generatePlayResourceTaskName = "generate${projectFlavorName}${buildTypeName}PlayResources"

            try {
                //Create GeneratePlayResourceTeak
                def generateTask = project.tasks.create(generatePlayResourceTaskName, GeneratePlayResourceTask)
                generateTask.flavor = StringUtils.uncapitalize(projectFlavorName)

                def generateOutput = "${project.getProjectDir().toString()}/build/outputs/play/${variant.name}"
                generateTask.outputFolder = new File(generateOutput)

                def zipalignTask = project.tasks."$zipalignTaskName"
                def manifestTask = project.tasks."$manifestTaskName"

                def publishTask = project.tasks.create(publishTaskName, PlayPublishTask)

                publishTask.extension = extension
                publishTask.apkFile = zipalignTask.outputFile
                publishTask.manifestFile = manifestTask.manifestOutputFile
                publishTask.inputFolder = generateTask.outputFolder

                publishTask.dependsOn project.tasks."generate${projectFlavorName}${buildTypeName}PlayResources"
                publishTask.dependsOn project.tasks."assemble$projectFlavorName$buildTypeName"
            } catch (MissingPropertyException e) {
                log.info("Could not find task ${zipalignTaskName}. Did you specify a signinConfig for the variation $projectFlavorName$buildTypeName?")
            }

        }
    }

}
