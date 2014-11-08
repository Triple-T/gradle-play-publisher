package de.triplet.gradle.play

import com.android.build.gradle.AppPlugin
import org.apache.commons.lang.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

class PlayPublisherPlugin implements Plugin<Project> {

    public static final String PLAY_STORE_GROUP = "Play Store"

    @Override
    void apply(Project project) {
        def log = project.logger

        def hasAppPlugin = project.plugins.find { p -> p instanceof AppPlugin }
        if (!hasAppPlugin) {
            throw new IllegalStateException("The 'com.android.application' plugin is required.")
        }

        def extension = project.extensions.create('play', PlayPublisherPluginExtension)

        project.android.applicationVariants.all { variant ->
            if (!variant.buildType.name.equals("release")) {
                log.debug("Skipping build type ${variant.buildType.name}.")
                return
            }

            def buildTypeName = variant.buildType.name.capitalize()

            def productFlavorNames = variant.productFlavors.collect { it.name.capitalize() }
            if (productFlavorNames.isEmpty()) {
                productFlavorNames = [""]
            }
            def productFlavorName = productFlavorNames.join('')
            def flavor = StringUtils.uncapitalize(productFlavorName)

            def variationName = "${productFlavorName}${buildTypeName}"

            def bootstrapTaskName = "bootstrap${variationName}PlayResources"
            def playResourcesTaskName = "generate${variationName}PlayResources"
            def publishApkTaskName = "publishApk${variationName}"
            def publishListingTaskName = "publishListing${variationName}"
            def publishTaskName = "publish${variationName}"

            def variantData = variant.variantData

            if (!variantData.zipAlignEnabled) {
                log.info("Could not find ZipAlign task. Did you specify a signingConfig for the variation ${variationName}?")
                return
            }

            def outputData = variant.outputs[0]
            def zipalignTask = outputData.zipAlign
            def manifestTask = outputData.processManifest
            def assembleTask = outputData.assemble

            // Create and configure bootstrap task for this variant.
            def bootstrapTask = project.tasks.create(bootstrapTaskName, BootstrapTask)
            bootstrapTask.extension = extension
            bootstrapTask.manifestFile = manifestTask.manifestOutputFile
            if (StringUtils.isNotEmpty(flavor)) {
                bootstrapTask.outputFolder = new File(project.getProjectDir(), "src/${flavor}/play")
            } else {
                bootstrapTask.outputFolder = new File(project.getProjectDir(), "src/main/play")
            }
            bootstrapTask.description = "Downloads the play store listing for the ${variationName} build"
            bootstrapTask.group = PLAY_STORE_GROUP

            // Create and configure task to collect the play store resources.
            def playResourcesTask = project.tasks.create(playResourcesTaskName, GeneratePlayResourcesTask)
            playResourcesTask.inputs.file(new File(project.getProjectDir(), "src/main/play"))
            if (StringUtils.isNotEmpty(flavor)) {
                playResourcesTask.inputs.file(new File(project.getProjectDir(), "src/${flavor}/play"))
            }
            playResourcesTask.outputFolder = new File(project.getProjectDir(), "build/outputs/play/${variant.name}")
            playResourcesTask.description = "Collects play store resources for the ${variationName} build"
            playResourcesTask.group = PLAY_STORE_GROUP

            // Create and configure publisher apk task for this variant.
            def publishApkTask = project.tasks.create(publishApkTaskName, PlayPublishApkTask)
            publishApkTask.extension = extension
            publishApkTask.apkFile = zipalignTask.outputFile
            publishApkTask.manifestFile = manifestTask.manifestOutputFile
            publishApkTask.inputFolder = playResourcesTask.outputFolder
            publishApkTask.description = "Uploads the APK for the ${variationName} build"
            publishApkTask.group = PLAY_STORE_GROUP

            // Create and configure publisher meta task for this variant
            def publishListingTask = project.tasks.create(publishListingTaskName, PlayPublishListingTask)
            publishListingTask.extension = extension
            publishListingTask.manifestFile = manifestTask.manifestOutputFile
            publishListingTask.inputFolder = playResourcesTask.outputFolder
            publishListingTask.description = "Updates the play store listing for the ${variationName} build"
            publishListingTask.group = PLAY_STORE_GROUP

            def publishTask = project.tasks.create(publishTaskName)
            publishTask.description = "Updates APK and play store listing for the ${variationName} build"
            publishTask.group = PLAY_STORE_GROUP

            // Attach tasks to task graph.
            bootstrapTask.dependsOn manifestTask
            publishTask.dependsOn publishApkTask
            publishTask.dependsOn publishListingTask
            publishListingTask.dependsOn playResourcesTask
            publishListingTask.dependsOn manifestTask
            publishApkTask.dependsOn playResourcesTask
            publishApkTask.dependsOn assembleTask
        }
    }

}
