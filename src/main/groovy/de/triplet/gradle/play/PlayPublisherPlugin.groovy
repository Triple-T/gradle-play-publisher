package de.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class PlayPublisherPlugin implements Plugin<Project> {

    static final PLAY_STORE_GROUP = 'Play Store'
    static final RESOURCES_OUTPUT_PATH = 'build/outputs/play'

    @Override
    void apply(Project project) {
        def log = project.logger

        if (!project.plugins.any { p -> p instanceof AppPlugin }) {
            throw new IllegalStateException('The \'com.android.application\' plugin is required.')
        }

        def android = project.extensions.getByType(AppExtension)
        def extension = project.extensions.create('play', PlayPublisherPluginExtension)

        android.extensions.playAccountConfigs = project.container(PlayAccountConfig)

        android.defaultConfig.ext.playAccountConfig = null

        android.productFlavors.whenObjectAdded { flavor ->
            flavor.ext.playAccountConfig = android.defaultConfig.ext.playAccountConfig
        }

        android.applicationVariants.whenObjectAdded { variant ->
            if (variant.buildType.isDebuggable()) {
                log.debug("Skipping debuggable build type ${variant.buildType.name}.")
                return
            }

            def flavorAccountConfig = variant.productFlavors.find { it.playAccountConfig }?.playAccountConfig
            def defaultAccountConfig = android.defaultConfig.ext.playAccountConfig
            def playAccountConfig = flavorAccountConfig ?: defaultAccountConfig

            def bootstrapTaskName = "bootstrap${variant.name.capitalize()}PlayResources"
            def playResourcesTaskName = "generate${variant.name.capitalize()}PlayResources"
            def publishApkTaskName = "publishApk${variant.name.capitalize()}"
            def publishListingTaskName = "publishListing${variant.name.capitalize()}"
            def publishTaskName = "publish${variant.name.capitalize()}"

            // Create and configure bootstrap task for this variant.
            def bootstrapTask = project.tasks.create(bootstrapTaskName, BootstrapTask)
            bootstrapTask.extension = extension
            bootstrapTask.variant = variant
            if (!variant.flavorName.isEmpty()) {
                bootstrapTask.outputFolder = project.file("src/${variant.flavorName}/play")
            } else {
                bootstrapTask.outputFolder = project.file('src/main/play')
            }
            bootstrapTask.playAccountConfig = playAccountConfig
            bootstrapTask.description = "Downloads the play store listing for the ${variant.name.capitalize()} build. No download of image resources. See #18."
            bootstrapTask.group = PLAY_STORE_GROUP

            // Create and configure task to collect the play store resources.
            def playResourcesTask = project.tasks.create(playResourcesTaskName, GeneratePlayResourcesTask)
            playResourcesTask.inputs.file(project.file('src/main/play'))
            if (!variant.flavorName.isEmpty()) {
                playResourcesTask.inputs.file(project.file("src/${variant.flavorName}/play"))
            }
            playResourcesTask.inputs.file(project.file("src/${variant.buildType.name}/play"))
            playResourcesTask.inputs.file(project.file("src/${variant.name}/play"))
            playResourcesTask.outputFolder = project.file("${RESOURCES_OUTPUT_PATH}/${variant.name}")
            playResourcesTask.description = "Collects play store resources for the ${variant.name.capitalize()} build"
            playResourcesTask.group = PLAY_STORE_GROUP

            // Create and configure publisher meta task for this variant
            def publishListingTask = project.tasks.create(publishListingTaskName, PlayPublishListingTask)
            publishListingTask.extension = extension
            publishListingTask.playAccountConfig = playAccountConfig
            publishListingTask.variant = variant
            publishListingTask.inputFolder = playResourcesTask.outputFolder
            publishListingTask.description = "Updates the play store listing for the ${variant.name.capitalize()} build"
            publishListingTask.group = PLAY_STORE_GROUP

            // Attach tasks to task graph.
            publishListingTask.dependsOn playResourcesTask

            if (variant.isSigningReady()) {
                // Create and configure publisher apk task for this variant.
                def publishApkTask = project.tasks.create(publishApkTaskName, PlayPublishApkTask)
                publishApkTask.extension = extension
                publishApkTask.playAccountConfig = playAccountConfig
                publishApkTask.variant = variant
                publishApkTask.inputFolder = playResourcesTask.outputFolder
                publishApkTask.description = "Uploads the APK for the ${variant.name.capitalize()} build"
                publishApkTask.group = PLAY_STORE_GROUP

                def publishTask = project.tasks.create(publishTaskName)
                publishTask.description = "Updates APK and play store listing for the ${variant.name.capitalize()} build"
                publishTask.group = PLAY_STORE_GROUP

                // Attach tasks to task graph.
                publishTask.dependsOn publishApkTask
                publishTask.dependsOn publishListingTask
                publishApkTask.dependsOn playResourcesTask
                publishApkTask.dependsOn variant.assemble
            } else {
                log.warn("Signing not ready. Did you specify a signingConfig for the variation ${variant.name.capitalize()}?")
            }
        }
    }

}
