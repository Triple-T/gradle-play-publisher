package de.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.builder.model.BaseConfig
import groovy.lang.GroovyObject
import org.gradle.api.Plugin
import org.gradle.api.Project

class PlayPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val log = project.logger
        val android = project.extensions.getByType(AppExtension::class.java)
                ?: throw IllegalStateException("The 'com.android.application' plugin is required.")
        val extension = project.extensions.create("play", PlayPublisherPluginExtension::class.java)

        android.extensions.add("playAccountConfigs", project.container(PlayAccountConfig::class.java))

        android.defaultConfig.extras.set("playAccountConfig", null)

        android.productFlavors.whenObjectAdded { flavor ->
            //flavor.ext.playAccountConfig = android.defaultConfig.ext.playAccountConfig
            flavor.extras.set("playAccountConfig", android.defaultConfig.extras.get("playAccountConfig"))
        }

        val bootstrapAllTask = project.task("bootstrapAll").apply {
            description = "Downloads the play store listing for all initiated variants. No download of image resources. See #18."
            group = PLAY_STORE_GROUP
        }
        val playResourcesAllTask = project.task("generateAll").apply {
            description = "Collects play store resources for all initiated variants."
            group = PLAY_STORE_GROUP
        }
        val publishAllTask = project.task("publishAll").apply {
            description = "Updates APK and play store listing for all initiated variants"
            group = PLAY_STORE_GROUP
        }
        val publishApkAllTask = project.task("publishApkAll").apply {
            description = "Uploads the APK for all initiated variants"
            group = PLAY_STORE_GROUP
        }
        val publishListingAllTask = project.task("publishListingAll").apply {
            description = "Updates the play store listing for all initiated variants"
            group = PLAY_STORE_GROUP
        }


        android.applicationVariants.whenObjectAdded { variant ->
            if (variant.buildType.isDebuggable) {
                log.debug("Skipping debuggable build type ${variant.buildType.name}.")
                return@whenObjectAdded
            }

            val flavorAccountConfig = variant.productFlavors
                    .map { it as BaseConfig as GroovyObject }
                    .mapNotNull { it.getProperty("playAccountConfig") }
                    .firstOrNull() as? PlayAccountConfig
            val defaultAccountConfig = android.defaultConfig.extras.get("playAccountConfig") as? PlayAccountConfig
            val playAccountConfig = flavorAccountConfig ?: defaultAccountConfig ?: PlayAccountConfig()
            val capName = variant.name.capitalize()

            val bootstrapTaskName = "bootstrap${capName}PlayResources"
            val playResourcesTaskName = "generate${capName}PlayResources"
            val autoIncrementVersionCodeTaskName = "autoIncrement${capName}VersionCode"
            val publishApkTaskName = "publishApk$capName"
            val publishListingTaskName = "publishListing$capName"
            val publishTaskName = "publish$capName"

            // Create and configure bootstrap task for this variant.
            val bootstrapTask = project.tasks.create(bootstrapTaskName, BootstrapTask::class.java).apply {
                this.extension = extension
                this.variant = variant
                this.playAccountConfig = playAccountConfig
                group = PLAY_STORE_GROUP
                description = "Downloads the play store listing for the $capName build. No download of image resources. See #18."
                outputFolder = project.file("src/${variant.flavorName.orDefault("main")}/play")
            }
            bootstrapAllTask.dependsOn(bootstrapTask)

            // Create and configure task to collect the play store resources.
            val playResourcesTask = project.tasks.create(playResourcesTaskName, GeneratePlayResourcesTask::class.java).apply {
                inputs.file(project.file("src/main/play"))
                if (!variant.flavorName.isEmpty()) {
                    inputs.file(project.file("src/${variant.flavorName}/play"))
                }
                inputs.file(project.file("src/${variant.buildType.name}/play"))
                inputs.file(project.file("src/${variant.name}/play"))

                outputFolder = project.file("$RESOURCES_OUTPUT_PATH/${variant.name}")
                description = "Collects play store resources for the $capName build"
                group = PLAY_STORE_GROUP
            }
            playResourcesAllTask.dependsOn(playResourcesTask)

            // Create and configure task to auto increment the play store version code
            val autoIncrementVersionCodeTask = project.tasks.create(
                    autoIncrementVersionCodeTaskName, AutoIncrementVersionCodeTask::class.java).apply {
                this.extension = extension
                this.playAccountConfig = playAccountConfig
                this.variant = variant
                setOnlyIf({ extension.autoIncrementVersion })
                description = "Retrieves the latest play store version code, increments and then adds it to the $capName build."
                group = PLAY_STORE_GROUP
            }
            variant.preBuild.dependsOn(autoIncrementVersionCodeTask)

            // Create and configure publisher meta task for this variant
            val publishListingTask = project.tasks.create(publishListingTaskName, PlayPublishListingTask::class.java).apply {
                this.extension = extension
                this.playAccountConfig = playAccountConfig
                this.variant = variant
                inputFolder = playResourcesTask.outputFolder
                description = "Updates the play store listing for the $capName build"
                group = PLAY_STORE_GROUP
            }

            // Attach tasks to task graph.
            publishListingTask.dependsOn(playResourcesTask)
            publishListingAllTask.dependsOn(publishListingTask)

            if (variant.isSigningReady) {
                // Create and configure publisher apk task for this variant.
                val publishApkTask = project.tasks.create(publishApkTaskName, PlayPublishApkTask::class.java).apply {
                    this.extension = extension
                    this.playAccountConfig = playAccountConfig
                    this.variant = variant
                    inputFolder = playResourcesTask.outputFolder
                    description = "Uploads the APK for the $capName build"
                    group = PLAY_STORE_GROUP
                }

                val publishTask = project.tasks.create(publishTaskName).apply {
                    description = "Updates APK and play store listing for the $capName build"
                    group = PLAY_STORE_GROUP
                }

                // Attach tasks to task graph.
                publishTask.dependsOn(publishApkTask)
                publishTask.dependsOn(publishListingTask)
                publishAllTask.dependsOn(publishTask)

                publishApkTask.dependsOn(playResourcesTask)
                publishApkTask.dependsOn(variant.assemble)
                publishApkAllTask.dependsOn(publishApkTask)
            } else {
                log.warn("Signing not ready. Did you specify a signingConfig for the variation $capName?")
            }
        }
    }
}
