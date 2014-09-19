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

            def projectFlavorName = projectFlavorNames.join('')

            def zipalignTaskName = "zipalign$projectFlavorName$buildTypeName"
            def manifestTaskName = "process${projectFlavorName}${buildTypeName}Manifest"
            def playResourcesTaskName = "generate${projectFlavorName}${buildTypeName}PlayResources"
            def publishApkTaskName = "publishApk$projectFlavorName$buildTypeName"
            def publishListingTaskName = "publishListing$projectFlavorName$buildTypeName"
            def publishTaskName = "publish$projectFlavorName$buildTypeName"

            try {
                // Find Android tasks to use their outputs.
                def zipalignTask = project.tasks."$zipalignTaskName"
                def manifestTask = project.tasks."$manifestTaskName"

                // Create task to collect the play store resources.
                def playResourcesTask = project.tasks.create(playResourcesTaskName, GeneratePlayResourcesTask)

                // Configure the inputs and outputs
                def configurations = []
                configurations << "main"

                def flavor = StringUtils.uncapitalize(projectFlavorName)
                if (!StringUtils.isEmpty(flavor)) {
                    configurations << flavor
                }
                configurations.each { c ->
                    playResourcesTask.inputs.file(new File(project.getProjectDir(), "src/${c}/play"))
                }

                def playResourcesOutput = new File(project.getProjectDir(), "build/outputs/play/${variant.name}")
                playResourcesTask.outputFolder = playResourcesOutput

                // Create and configure publisher apk task for this variant.
                def publishApkTask = project.tasks.create(publishApkTaskName, PlayPublishApkTask)
                publishApkTask.extension = extension
                publishApkTask.apkFile = zipalignTask.outputFile
                publishApkTask.manifestFile = manifestTask.manifestOutputFile
                publishApkTask.inputFolder = playResourcesTask.outputFolder

                // Create and configure publisher meta task for this variant
                def publishListingTask = project.tasks.create(publishListingTaskName, PlayPublishListingTask)
                publishListingTask.extension = extension
                publishListingTask.manifestFile = manifestTask.manifestOutputFile
                publishListingTask.inputFolder = playResourcesTask.outputFolder

                def publishTask = project.tasks.create(publishTaskName)

                // Attach tasks to task graph.
                publishTask.dependsOn publishApkTask
                publishTask.dependsOn publishListingTask
                publishListingTask.dependsOn playResourcesTask
                publishListingTask.dependsOn manifestTask
                publishApkTask.dependsOn playResourcesTask
                publishApkTask.dependsOn project.tasks."assemble$projectFlavorName$buildTypeName"

            } catch (MissingPropertyException e) {
                log.info("Could not find task ${zipalignTaskName}. Did you specify a signinConfig for the variation $projectFlavorName$buildTypeName?")
            }

        }
    }

}
