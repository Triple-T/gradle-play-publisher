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
            if (variant.buildType.isDebuggable()) {
                log.debug("Skipping debuggable build type ${variant.buildType.name}.")
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
            def publishObbTaskName = "publishObb${variationName}"
            def publishListingTaskName = "publishListing${variationName}"
            def publishTaskName = "publish${variationName}"

            // Create and configure bootstrap task for this variant.
            def bootstrapTask = project.tasks.create(bootstrapTaskName, BootstrapTask)
            bootstrapTask.extension = extension
            bootstrapTask.variant = variant
            if (StringUtils.isNotEmpty(flavor)) {
                bootstrapTask.outputFolder = new File(project.projectDir, "src/${flavor}/play")
            } else {
                bootstrapTask.outputFolder = new File(project.projectDir, "src/main/play")
            }
            bootstrapTask.description = "Downloads the play store listing for the ${variationName} build. No download of image resources. See #18."
            bootstrapTask.group = PLAY_STORE_GROUP

            // Create and configure task to collect the play store resources.
            def playResourcesTask = project.tasks.create(playResourcesTaskName, GeneratePlayResourcesTask)

            playResourcesTask.inputs.file(new File(project.projectDir, "src/main/play"))
            if (StringUtils.isNotEmpty(flavor)) {
                playResourcesTask.inputs.file(new File(project.projectDir, "src/${flavor}/play"))
            }
            playResourcesTask.inputs.file(new File(project.projectDir, "src/${variant.buildType.name}/play"))
            if (StringUtils.isNotEmpty(flavor)) {
                playResourcesTask.inputs.file(new File(project.projectDir, "src/${variant.name}/play"))
            }

            playResourcesTask.outputFolder = new File(project.projectDir, "build/outputs/play/${variant.name}")
            playResourcesTask.description = "Collects play store resources for the ${variationName} build"
            playResourcesTask.group = PLAY_STORE_GROUP

            // Create and configure publisher meta task for this variant
            def publishListingTask = project.tasks.create(publishListingTaskName, PlayPublishListingTask)
            publishListingTask.extension = extension
            publishListingTask.variant = variant
            publishListingTask.inputFolder = playResourcesTask.outputFolder
            publishListingTask.description = "Updates the play store listing for the ${variationName} build"
            publishListingTask.group = PLAY_STORE_GROUP

            // Attach tasks to task graph.
            publishListingTask.dependsOn playResourcesTask

            if (variant.isSigningReady()) {
                // Create and configure publisher apk task for this variant.
                def publishApkTask = project.tasks.create(publishApkTaskName, PlayPublishApkTask)
                publishApkTask.extension = extension
                publishApkTask.variant = variant
                publishApkTask.inputFolder = playResourcesTask.outputFolder
                publishApkTask.description = "Uploads the APK for the ${variationName} build"
                publishApkTask.group = PLAY_STORE_GROUP

                def publishObbTask = project.tasks.create(publishObbTaskName, PlayPublishObbTask)
                publishObbTask.extension = extension
                publishObbTask.variant = variant
                publishObbTask.inputFolder = playResourcesTask.outputFolder
                publishObbTask.description = "Uploads the OBB for the ${variationName} build"
                publishObbTask.group = PLAY_STORE_GROUP

                def publishTask = project.tasks.create(publishTaskName)
                publishTask.description = "Updates APK and play store listing for the ${variationName} build"
                publishTask.group = PLAY_STORE_GROUP

                // Attach tasks to task graph.
                publishTask.dependsOn publishApkTask
                publishTask.dependsOn publishObbTask
                publishTask.dependsOn publishListingTask
                publishApkTask.dependsOn playResourcesTask
                publishApkTask.dependsOn project.tasks."assemble${variationName}"
                publishObbTask.dependsOn playResourcesTask
            } else {
                log.warn("Signing not ready. Did you specify a signingConfig for the variation ${variationName}?")
            }
        }
    }

}
