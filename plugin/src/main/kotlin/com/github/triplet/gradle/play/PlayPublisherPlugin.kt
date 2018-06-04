package com.github.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.internal.ACCOUNT_CONFIG
import com.github.triplet.gradle.play.internal.AccountConfig
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.RESOURCES_OUTPUT_PATH
import com.github.triplet.gradle.play.internal.get
import com.github.triplet.gradle.play.internal.newTask
import com.github.triplet.gradle.play.internal.nullOrFull
import com.github.triplet.gradle.play.internal.set
import groovy.lang.GroovyObject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import java.io.File

class PlayPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val android = requireNotNull(project.extensions.get<AppExtension>()) {
            "The 'com.android.application' plugin is required."
        }
        val extension: PlayPublisherExtension =
                project.extensions.create(PLAY_PATH, PlayPublisherExtension::class.java)

        val bootstrapAllTask = project.newTask<Task>(
                "bootstrapAll",
                "Downloads the Play Store listing metadata for all variants."
        )
        val playResourcesAllTask = project.newTask<Task>(
                "generateAll",
                "Collects Play Store resources for all variants."
        )
        val publishAllTask = project.newTask<Task>(
                "publishAll",
                "Uploads APK or App Bundle and all Play Store metadata for every variant."
        )
        val publishApkAllTask = project.newTask<Task>(
                "publishApkAll",
                "Uploads APK for every variant."
        )
        val publishListingAllTask = project.newTask<Task>(
                "publishListingAll",
                "Uploads all Play Store metadata for every variant."
        )

        project.initPlayAccountConfigs(android)
        android.applicationVariants.whenObjectAdded { variant ->
            if (variant.buildType.isDebuggable) {
                project.logger.info("Skipping debuggable build type ${variant.buildType.name}.")
                return@whenObjectAdded
            }

            val accountConfig = android.getAccountConfig(variant) ?: extension
            val variantName = variant.name.capitalize()

            if (!variant.isSigningReady) {
                project.logger.error(
                        "Signing not ready. Be sure to specify a signingConfig for $variantName")
            }
            accountConfig.run {
                check(jsonFile != null || pk12File != null && serviceAccountEmail != null) {
                    "No credentials provided"
                }
            }

            fun PlayPublishTaskBase.init() {
                this.extension = extension
                this.variant = variant
                this.accountConfig = accountConfig
            }

            project.newTask<BootstrapTask>(
                    "bootstrap${variantName}PlayResources",
                    "Downloads the Play Store listing metadata for $variantName."
            ) {
                init()
                outputFolder =
                        project.file("src/${variant.flavorName.nullOrFull() ?: "main"}/$PLAY_PATH")

                bootstrapAllTask.dependsOn(this)
            }

            val playResourcesTask = project.newTask<GeneratePlayResourcesTask>(
                    "generate${variantName}PlayResources",
                    "Collects Play Store resources for $variantName."
            ) {
                inputs.file("src/main/$PLAY_PATH")
                if (variant.flavorName.isNotEmpty()) {
                    inputs.file("src/${variant.flavorName}/$PLAY_PATH")
                }
                inputs.file("src/${variant.buildType.name}/$PLAY_PATH")
                inputs.file("src/${variant.name}/$PLAY_PATH")

                outputFolder = File(project.buildDir, "$RESOURCES_OUTPUT_PATH/${variant.name}")

                playResourcesAllTask.dependsOn(this)
            }
            val publishListingTask = project.newTask<PublishListingTask>(
                    "publishListing$variantName",
                    "Uploads all Play Store metadata for $variantName."
            ) {
                init()
                inputFolder = playResourcesTask.outputFolder

                dependsOn(playResourcesTask)
                publishListingAllTask.dependsOn(this)
            }

            val publishApkTask = project.newTask<PublishApkTask>(
                    "publishApk$variantName",
                    "Uploads APK for $variantName."
            ) {
                init()
                inputFolder = playResourcesTask.outputFolder

                dependsOn(playResourcesTask)
                dependsOn(variant.assemble)
                publishApkAllTask.dependsOn(this)
            }

            project.newTask<Task>(
                    "publish$variantName",
                    "Uploads all Play Store metadata for $variantName."
            ) {
                dependsOn(publishApkTask)
                dependsOn(publishListingTask)
                publishAllTask.dependsOn(this)
            }
        }
    }

    private fun Project.initPlayAccountConfigs(android: AppExtension) {
        (android as ExtensionAware).extensions.add(
                "playAccountConfigs", container(PlayAccountConfigExtension::class.java))
        android.defaultConfig[ACCOUNT_CONFIG] = null
        android.productFlavors.whenObjectAdded {
            it[ACCOUNT_CONFIG] = android.defaultConfig[ACCOUNT_CONFIG]
        }
    }

    private fun AppExtension.getAccountConfig(variant: ApplicationVariant): AccountConfig? {
        val flavorAccountConfig = variant.productFlavors
                .mapNotNull { (it as GroovyObject).getProperty(ACCOUNT_CONFIG) }
                .singleOrNull() as? AccountConfig
        val defaultAccountConfig = defaultConfig[ACCOUNT_CONFIG] as? AccountConfig

        return flavorAccountConfig ?: defaultAccountConfig
    }
}
