package com.github.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.api.InstallableVariantImpl
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.configure
import com.github.triplet.gradle.play.internal.dependsOnCompat
import com.github.triplet.gradle.play.internal.mergeWith
import com.github.triplet.gradle.play.internal.newTask
import com.github.triplet.gradle.play.internal.requireCreds
import com.github.triplet.gradle.play.internal.validate
import com.github.triplet.gradle.play.internal.validateRuntime
import com.github.triplet.gradle.play.tasks.Bootstrap
import com.github.triplet.gradle.play.tasks.GenerateResources
import com.github.triplet.gradle.play.tasks.ProcessPackageMetadata
import com.github.triplet.gradle.play.tasks.PromoteRelease
import com.github.triplet.gradle.play.tasks.PublishApk
import com.github.triplet.gradle.play.tasks.PublishBundle
import com.github.triplet.gradle.play.tasks.PublishListing
import com.github.triplet.gradle.play.tasks.PublishProducts
import com.github.triplet.gradle.play.tasks.internal.BootstrapLifecycleHelperTask
import com.github.triplet.gradle.play.tasks.internal.BootstrapOptionsHolder
import com.github.triplet.gradle.play.tasks.internal.LifecycleHelperTask
import com.github.triplet.gradle.play.tasks.internal.PlayPublishTaskBase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the

@Suppress("unused") // Used by Gradle
class PlayPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        validateRuntime()

        val android = requireNotNull(project.the<AppExtension>()) {
            "The 'com.android.application' plugin is required."
        }
        val baseExtension: PlayPublisherExtension =
                project.extensions.create(PLAY_PATH, PlayPublisherExtension::class.java)
        val extensionContainer = project.container(PlayPublisherExtension::class.java)

        val bootstrapAllTask = project.newTask<BootstrapLifecycleHelperTask>(
                "bootstrap",
                "Downloads the Play Store listing metadata for all variants."
        ) { extension = baseExtension }
        val publishAllTask = project.newTask<LifecycleHelperTask>(
                "publish",
                "Uploads APK or App Bundle and all Play Store metadata for every variant."
        ) { extension = baseExtension }
        val publishApkAllTask = project.newTask<LifecycleHelperTask>(
                "publishApk",
                "Uploads APK for every variant."
        ) { extension = baseExtension }
        val publishBundleAllTask = project.newTask<LifecycleHelperTask>(
                "publishBundle",
                "Uploads App Bundle for every variant."
        ) { extension = baseExtension }
        val promoteReleaseAllTask = project.newTask<LifecycleHelperTask>(
                "promoteArtifact",
                "Promotes a release for every variant."
        ) { extension = baseExtension }
        val publishListingAllTask = project.newTask<LifecycleHelperTask>(
                "publishListing",
                "Uploads all Play Store metadata for every variant."
        ) { extension = baseExtension }
        val publishProductsAllTask = project.newTask<LifecycleHelperTask>(
                "publishProducts",
                "Uploads all Play Store in-app products for every variant."
        ) { extension = baseExtension }

        (android as ExtensionAware).extensions.add("playConfigs", extensionContainer)
        BootstrapOptionsHolder.reset()
        android.applicationVariants.whenObjectAdded {
            if (buildType.isDebuggable) {
                project.logger.info("Skipping debuggable build type ${buildType.name}.")
                return@whenObjectAdded
            }

            val extension = productFlavors.mapNotNull {
                extensionContainer.findByName(it.name)
            }.singleOrNull().mergeWith(baseExtension)
            val variantName = name.capitalize()

            if (!isSigningReady) {
                project.logger.error(
                        "Signing not ready. Be sure to specify a signingConfig for $variantName")
            }
            extension.run {
                if (requireCreds().extension.equals("json", true)) {
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
            }

            val bootstrapTask = project.newTask<Bootstrap>(
                    "bootstrap${variantName}PlayResources",
                    "Downloads the Play Store listing metadata for $variantName."
            ) { init() }
            bootstrapAllTask.configure { dependsOnCompat(bootstrapTask) }

            val playResourcesTask = project.newTask<GenerateResources>(
                    "generate${variantName}PlayResources",
                    "Collects Play Store resources for $variantName.",
                    null
            ) {
                variant = this@whenObjectAdded
            }

            val publishListingTask = project.newTask<PublishListing>(
                    "publish${variantName}Listing",
                    "Uploads all Play Store metadata for $variantName."
            ) {
                init()
                resDir = playResourcesTask.get().resDir

                dependsOnCompat(playResourcesTask)
            }
            publishListingAllTask.configure { dependsOnCompat(publishListingTask) }
            // TODO Remove in v3.0
            project.newTask("publishListing$variantName", "", null) {
                dependsOnCompat(publishListingTask)
                doFirst { logger.warn("$name is deprecated, use ${publishListingTask.get().name} instead") }
            }

            val publishProductsTask = project.newTask<PublishProducts>(
                    "publish${variantName}Products",
                    "Uploads all Play Store in-app products for $variantName."
            ) {
                init()
                resDir = playResourcesTask.get().resDir

                dependsOnCompat(playResourcesTask)
            }
            publishProductsAllTask.configure { dependsOnCompat(publishProductsTask) }

            val processPackageMetadata = project.newTask<ProcessPackageMetadata>(
                    "process${variantName}Metadata",
                    "Processes packaging metadata for $variantName.",
                    null
            ) { init() }
            try {
                checkManifestProvider.configure { dependsOn(processPackageMetadata) }
            } catch (e: NoSuchMethodError) {
                @Suppress("DEPRECATION")
                checkManifest.dependsOnCompat(processPackageMetadata)
            }

            val publishApkTask = project.newTask<PublishApk>(
                    "publish${variantName}Apk",
                    "Uploads APK for $variantName."
            ) {
                init()
                resDir = playResourcesTask.get().resDir

                dependsOnCompat(processPackageMetadata)
                dependsOnCompat(playResourcesTask)
                try {
                    variant.assembleProvider
                } catch (e: NoSuchMethodError) {
                    @Suppress("DEPRECATION")
                    variant.assemble
                }?.let { dependsOn(it) }
                        ?: logger.warn("Assemble task not found. Publishing APKs may not work.")
            }
            publishApkAllTask.configure { dependsOnCompat(publishApkTask) }
            // TODO Remove in v3.0
            project.newTask("publishApk$variantName", "", null) {
                dependsOnCompat(publishApkTask)
                doFirst { logger.warn("$name is deprecated, use ${publishApkTask.get().name} instead") }
            }

            val publishBundleTask = project.newTask<PublishBundle>(
                    "publish${variantName}Bundle",
                    "Uploads App Bundle for $variantName."
            ) {
                init()
                resDir = playResourcesTask.get().resDir

                dependsOnCompat(processPackageMetadata)
                dependsOnCompat(playResourcesTask)
                // Remove hack when AGP 3.2 reaches stable channel
                project.tasks.findByName(
                        (variant as InstallableVariantImpl).variantData.getTaskName("bundle", ""))
                        ?.let { dependsOn(it) }
                        ?: logger.warn("Bundle task not found, make sure to use " +
                                               "'com.android.tools.build:gradle' v3.2+. " +
                                               "Publishing App Bundles may not work.")
            }
            publishBundleAllTask.configure { dependsOnCompat(publishBundleTask) }

            val promoteReleaseTask = project.newTask<PromoteRelease>(
                    "promote${variantName}Artifact",
                    "Promotes a release for $variantName."
            ) {
                init()
                resDir = playResourcesTask.get().resDir

                dependsOnCompat(playResourcesTask)
            }
            promoteReleaseAllTask.configure { dependsOnCompat(promoteReleaseTask) }

            val publishTask = project.newTask<LifecycleHelperTask>(
                    "publish$variantName",
                    "Uploads APK or App Bundle and all Play Store metadata for $variantName."
            ) {
                this.extension = extension

                dependsOnCompat(
                        if (extension.defaultToAppBundles) publishBundleTask else publishApkTask)
                dependsOnCompat(publishListingTask)
                dependsOnCompat(publishProductsTask)
            }
            publishAllTask.configure { dependsOnCompat(publishTask) }

            project.afterEvaluate {
                extension.validate()
            }
        }
    }
}
