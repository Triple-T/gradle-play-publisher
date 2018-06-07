package com.github.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.internal.ACCOUNT_CONFIG
import com.github.triplet.gradle.play.internal.AccountConfig
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.flavorNameOrDefault
import com.github.triplet.gradle.play.internal.get
import com.github.triplet.gradle.play.internal.newTask
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.set
import com.github.triplet.gradle.play.internal.validate
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
        val extension = project.extensions.create(PLAY_PATH, PlayPublisherExtension::class.java)

        val bootstrapAllTask = project.newTask<Task>(
                "bootstrapAll",
                "Downloads the Play Store listing metadata for all variants."
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

            val accountConfig = android.getAccountConfig(variant)
            val variantName = variant.name.capitalize()

            fun PlayPublishTaskBase.init() {
                this.extension = extension
                this.variant = variant
                this.accountConfig = accountConfig ?: extension
            }

            project.newTask<BootstrapTask>(
                    "bootstrap${variantName}PlayResources",
                    "Downloads the Play Store listing metadata for $variantName."
            ) {
                init()
                srcDir = project.file("src/${variant.flavorNameOrDefault}/$PLAY_PATH")

                bootstrapAllTask.dependsOn(this)
            }

            val playResourcesTask = project.newTask<GenerateResourcesTask>(
                    "generate${variantName}PlayResources",
                    "Collects Play Store resources for $variantName.",
                    null
            ) {
                this.variant = variant
                resDir = File(project.buildDir, "${variant.playPath}/res")
            }
            val publishListingTask = project.newTask<PublishListingTask>(
                    "publishListing$variantName",
                    "Uploads all Play Store metadata for $variantName."
            ) {
                init()
                resDir = playResourcesTask.resDir

                dependsOn(playResourcesTask)
                publishListingAllTask.dependsOn(this)
            }

            if (variant.isSigningReady) {
                val processPackageMetadata = project.newTask<ProcessPackageMetadataTask>(
                        "processPackageMetadata$variantName",
                        "Processes packaging metadata for $variantName.",
                        null
                ) {
                    init()

                    variant.checkManifest.dependsOn(this)
                }

                val publishApkTask = project.newTask<PublishApkTask>(
                        "publishApk$variantName",
                        "Uploads APK for $variantName."
                ) {
                    init()
                    resDir = playResourcesTask.resDir

                    dependsOn(processPackageMetadata)
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
            } else {
                project.logger.error(
                        "Signing not ready. Be sure to specify a signingConfig for $variantName?")
            }
        }

        project.afterEvaluate {
            extension.validate()
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
