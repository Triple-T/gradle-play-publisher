package com.github.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.api.InstallableVariantImpl
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
import com.github.triplet.gradle.play.internal.validateRuntime
import com.github.triplet.gradle.play.tasks.Bootstrap
import com.github.triplet.gradle.play.tasks.GenerateResources
import com.github.triplet.gradle.play.tasks.ProcessPackageMetadata
import com.github.triplet.gradle.play.tasks.PublishApk
import com.github.triplet.gradle.play.tasks.PublishBundle
import com.github.triplet.gradle.play.tasks.PublishListing
import groovy.lang.GroovyObject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the
import java.io.File

class PlayPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        validateRuntime()

        val android = requireNotNull(project.the<AppExtension>()) {
            "The 'com.android.application' plugin is required."
        }
        val extension: PlayPublisherExtension =
                project.extensions.create(PLAY_PATH, PlayPublisherExtension::class.java)

        val bootstrapAllTask = project.newTask<LifecycleHelperTask>(
                "bootstrap",
                "Downloads the Play Store listing metadata for all variants."
        ) { this.extension = extension }
        val publishAllTask = project.newTask<LifecycleHelperTask>(
                "publish",
                "Uploads APK or App Bundle and all Play Store metadata for every variant."
        ) { this.extension = extension }
        val publishApkAllTask = project.newTask<LifecycleHelperTask>(
                "publishApk",
                "Uploads APK for every variant."
        ) { this.extension = extension }
        val publishBundleAllTask = project.newTask<LifecycleHelperTask>(
                "publishBundle",
                "Uploads App Bundle for every variant."
        ) { this.extension = extension }
        val publishListingAllTask = project.newTask<LifecycleHelperTask>(
                "publishListing",
                "Uploads all Play Store metadata for every variant."
        ) { this.extension = extension }

        project.initPlayAccountConfigs(android)
        android.applicationVariants.whenObjectAdded {
            if (buildType.isDebuggable) {
                project.logger.info("Skipping debuggable build type ${buildType.name}.")
                return@whenObjectAdded
            }

            val accountConfig = android.getAccountConfig(this) ?: extension
            val variantName = name.capitalize()

            if (!isSigningReady) {
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
                this.variant = this@whenObjectAdded
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
                variant = this@whenObjectAdded
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
                variant.assemble?.let { dependsOn(it) }
                        ?: logger.warn("Assemble task not found. Publishing APKs may not work.")
                publishApkAllTask.dependsOn(this)

                // Remove in v3.0
                val new = this
                project.newTask<Task>("publishApk$variantName", "", null) {
                    dependsOn(new)
                    doFirst { logger.warn("$name is deprecated, use ${new.name} instead") }
                }
            }

            val publishBundleTask = project.newTask<PublishBundle>(
                    "publish${variantName}Bundle",
                    "Uploads App Bundle for $variantName."
            ) {
                init()
                resDir = playResourcesTask.resDir

                dependsOn(processPackageMetadata)
                dependsOn(playResourcesTask)
                // Remove hack when AGP 3.2 reaches stable channel
                project.tasks.findByName(
                        (variant as InstallableVariantImpl).variantData.getTaskName("bundle", ""))
                        ?.let { dependsOn(it) }
                        ?: logger.warn("Bundle task not found, make sure to use " +
                                               "'com.android.tools.build:gradle' v3.2+. " +
                                               "Publishing App Bundles may not work.")
                publishBundleAllTask.dependsOn(this)
            }

            project.newTask<LifecycleHelperTask>(
                    "publish$variantName",
                    "Uploads APK or App Bundle and all Play Store metadata for $variantName."
            ) {
                this.extension = extension

                dependsOn(
                        if (extension.defaultToAppBundles) publishBundleTask else publishApkTask)
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
            this[ACCOUNT_CONFIG] = android.defaultConfig[ACCOUNT_CONFIG]
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
