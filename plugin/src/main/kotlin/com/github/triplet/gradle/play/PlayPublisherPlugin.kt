package com.github.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.internal.ACCOUNT_CONFIG
import com.github.triplet.gradle.play.internal.AccountConfig
import com.github.triplet.gradle.play.internal.LifecycleHelperTask
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.flavorNameOrDefault
import com.github.triplet.gradle.play.internal.get
import com.github.triplet.gradle.play.internal.newTask
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.set
import com.github.triplet.gradle.play.internal.validate
import com.github.triplet.gradle.play.tasks.Bootstrap
import com.github.triplet.gradle.play.tasks.GenerateResources
import com.github.triplet.gradle.play.tasks.ProcessPackageMetadata
import com.github.triplet.gradle.play.tasks.PublishApk
import com.github.triplet.gradle.play.tasks.PublishListing
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

        val bootstrapAllTask = project.newTask<LifecycleHelperTask>(
                "bootstrap",
                "Downloads the Play Store listing metadata for all variants.",
                args = *arrayOf(extension)
        )
        val publishAllTask = project.newTask<LifecycleHelperTask>(
                "publish",
                "Uploads APK or App Bundle and all Play Store metadata for every variant.",
                args = *arrayOf(extension)
        )
        val publishApkAllTask = project.newTask<LifecycleHelperTask>(
                "publishApk",
                "Uploads APK for every variant.",
                args = *arrayOf(extension)
        )
        val publishListingAllTask = project.newTask<LifecycleHelperTask>(
                "publishListing",
                "Uploads all Play Store metadata for every variant.",
                args = *arrayOf(extension)
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
                if (_serviceAccountCredentials.extension.equals("json", true)) {
                    check(serviceAccountEmail == null) {
                        "Json credentials cannot specify a Service Account email"
                    }
                } else {
                    check(serviceAccountEmail != null) {
                        "PKCS12 credentials must also specify a Service Account email"
                    }
                }
            }

            fun PlayPublishTaskBase.init() {
                this.extension = extension
                this.variant = variant
                this.accountConfig = accountConfig
            }

            project.newTask<Bootstrap>(
                    "bootstrap${variantName}PlayResources",
                    "Downloads the Play Store listing metadata for $variantName."
            ) {
                init()
                srcDir = project.file("src/${variant.flavorNameOrDefault}/$PLAY_PATH")

                bootstrapAllTask.dependsOn(this)
            }

            val playResourcesTask = project.newTask<GenerateResources>(
                    "generate${variantName}PlayResources",
                    "Collects Play Store resources for $variantName.",
                    null
            ) {
                this.variant = variant
                init()
                resDir = File(project.buildDir, "${variant.playPath}/res")
            }

            val publishListingTask = project.newTask<PublishListing>(
                    "publish${variantName}Listing",
                    "Uploads all Play Store metadata for $variantName."
            ) {
                init()
                resDir = playResourcesTask.resDir

                dependsOn(playResourcesTask)
                publishListingAllTask.dependsOn(this)

                // Remove in v3.0
                val new = this
                project.newTask<Task>("publishListing$variantName", "", null) {
                    dependsOn(new)
                    doFirst { logger.warn("$name is deprecated, use ${new.name} instead") }
                }
            }

            val processPackageMetadata = project.newTask<ProcessPackageMetadata>(
                    "process${variantName}Metadata",
                    "Processes packaging metadata for $variantName.",
                    null
            ) {
                init()

                variant.checkManifest.dependsOn(this)
            }

            val publishApkTask = project.newTask<PublishApk>(
                    "publish${variantName}Apk",
                    "Uploads APK for $variantName."
            ) {
                init()
                resDir = playResourcesTask.resDir

                dependsOn(processPackageMetadata)
                dependsOn(playResourcesTask)
                dependsOn(variant.assemble)
                publishApkAllTask.dependsOn(this)

                // Remove in v3.0
                val new = this
                project.newTask<Task>("publishApk$variantName", "", null) {
                    dependsOn(new)
                    doFirst { logger.warn("$name is deprecated, use ${new.name} instead") }
                }
            }

            project.newTask<LifecycleHelperTask>(
                    "publish$variantName",
                    "Uploads all Play Store metadata for $variantName.",
                    args = *arrayOf(extension)
            ) {
                dependsOn(publishApkTask)
                dependsOn(publishListingTask)
                publishAllTask.dependsOn(this)
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
