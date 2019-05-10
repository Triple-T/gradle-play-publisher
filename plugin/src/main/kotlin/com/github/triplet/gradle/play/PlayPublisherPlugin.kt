package com.github.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.api.InstallableVariantImpl
import com.github.triplet.gradle.play.internal.PLAY_CONFIGS_PATH
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.mergeWith
import com.github.triplet.gradle.play.internal.newTask
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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

@Suppress("unused") // Used by Gradle
class PlayPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        validateRuntime()

        project.plugins.withType<AppPlugin> {
            applyInternal(project)
        }
    }

    private fun applyInternal(project: Project) {
        val baseExtension: PlayPublisherExtension =
                project.extensions.create(PLAY_PATH, PlayPublisherExtension::class.java)
        val extensionContainer = project.container(PlayPublisherExtension::class.java)

        val bootstrapAllTask = project.newTask<BootstrapLifecycleHelperTask>(
                "bootstrap",
                "Downloads the Play Store listing metadata for all variants.",
                arrayOf(baseExtension)
        )
        val publishAllTask = project.newTask<LifecycleHelperTask>(
                "publish",
                "Uploads APK or App Bundle and all Play Store metadata for every variant.",
                arrayOf(baseExtension)
        )
        val publishApkAllTask = project.newTask<LifecycleHelperTask>(
                "publishApk",
                "Uploads APK for every variant.",
                arrayOf(baseExtension)
        )
        val publishBundleAllTask = project.newTask<LifecycleHelperTask>(
                "publishBundle",
                "Uploads App Bundle for every variant.",
                arrayOf(baseExtension)
        )
        val promoteReleaseAllTask = project.newTask<LifecycleHelperTask>(
                "promoteArtifact",
                "Promotes a release for every variant.",
                arrayOf(baseExtension)
        )
        val publishListingAllTask = project.newTask<LifecycleHelperTask>(
                "publishListing",
                "Uploads all Play Store metadata for every variant.",
                arrayOf(baseExtension)
        )
        val publishProductsAllTask = project.newTask<LifecycleHelperTask>(
                "publishProducts",
                "Uploads all Play Store in-app products for every variant.",
                arrayOf(baseExtension)
        )

        val android = project.the<AppExtension>()
        (android as ExtensionAware).extensions.add(PLAY_CONFIGS_PATH, extensionContainer)
        BootstrapOptionsHolder.reset()
        android.applicationVariants.whenObjectAdded {
            val variantName = name.capitalize()

            if (buildType.isDebuggable) {
                val typeName = buildType.name
                if (typeName.equals("release", true)) {
                    project.logger.error(
                            "GPP cannot configure variant '$name' because it is debuggable")
                } else {
                    project.logger.info("Skipping debuggable build with type '$typeName'")
                }
                return@whenObjectAdded
            }

            val extension = (extensionContainer.findByName(name) ?: productFlavors.mapNotNull {
                extensionContainer.findByName(it.name)
            }.singleOrNull().let {
                it ?: extensionContainer.findByName(buildType.name)
            }).mergeWith(baseExtension)

            if (!extension.isEnabled) {
                project.logger.info("Gradle Play Publisher is disabled for variant '$name'.")
                return@whenObjectAdded
            }

            extension.apply {
                val creds = checkNotNull(_serviceAccountCredentials) {
                    "No credentials specified. Please read our docs for more details: " +
                            "https://github.com/Triple-T/gradle-play-publisher" +
                            "#authenticating-gradle-play-publisher"
                }
                if (creds.extension.equals("json", true)) {
                    check(serviceAccountEmail == null) {
                        "JSON credentials cannot specify a service account email."
                    }
                } else {
                    check(serviceAccountEmail != null) {
                        "PKCS12 credentials must specify a service account email."
                    }
                }
            }

            if (!isSigningReady && !outputsAreSigned) {
                project.logger.error(
                        "Signing not ready. " +
                                "Be sure to specify a signingConfig for variant '$name'.")
            }

            val bootstrapTask = project.newTask<Bootstrap>(
                    "bootstrap${variantName}PlayResources",
                    "Downloads the Play Store listing metadata for variant '$name'.",
                    arrayOf(extension, this)
            )
            bootstrapAllTask.configure { dependsOn(bootstrapTask) }

            val playResourcesTask = project.newTask<GenerateResources>(
                    "generate${variantName}PlayResources",
                    constructorArgs = arrayOf(this)
            )

            val publishListingTask = project.newTask<PublishListing>(
                    "publish${variantName}Listing",
                    "Uploads all Play Store metadata for variant '$name'.",
                    arrayOf(extension, this)
            ) {
                resDir = playResourcesTask.get().resDir

                dependsOn(playResourcesTask)
            }
            publishListingAllTask.configure { dependsOn(publishListingTask) }
            // TODO Remove in v3.0
            project.newTask("publishListing$variantName") {
                dependsOn(publishListingTask)
                doFirst { logger.warn("$name is deprecated, use ${publishListingTask.get().name} instead") }
            }

            val publishProductsTask = project.newTask<PublishProducts>(
                    "publish${variantName}Products",
                    "Uploads all Play Store in-app products for variant '$name'.",
                    arrayOf(extension, this)
            ) {
                resDir = playResourcesTask.get().resDir

                dependsOn(playResourcesTask)
            }
            publishProductsAllTask.configure { dependsOn(publishProductsTask) }

            val processPackageMetadata = project.newTask<ProcessPackageMetadata>(
                    "process${variantName}Metadata",
                    constructorArgs = arrayOf(extension, this)
            )
            checkManifestProvider.configure { dependsOn(processPackageMetadata) }
            generateBuildConfigProvider.configure { dependsOn(processPackageMetadata) }

            val publishApkTaskDependenciesHack = project.newTask(
                    "publish${variantName}ApkWrapper"
            ) {
                if (extension._artifactDir == null) {
                    dependsOn(processPackageMetadata)
                    assembleProvider?.let {
                        dependsOn(it)
                    } ?: logger.warn("Assemble task not found. Publishing APKs may not work.")
                }
            }
            val publishApkTask = project.newTask<PublishApk>(
                    "publish${variantName}Apk",
                    "Uploads APK for variant '$name'.",
                    arrayOf(extension, this)
            ) {
                resDir = playResourcesTask.get().resDir

                dependsOn(playResourcesTask)
                dependsOn(publishApkTaskDependenciesHack)
            }
            publishApkAllTask.configure { dependsOn(publishApkTask) }
            // TODO Remove in v3.0
            project.newTask("publishApk$variantName") {
                dependsOn(publishApkTask)
                doFirst { logger.warn("$name is deprecated, use ${publishApkTask.get().name} instead") }
            }

            val publishBundleTaskDependenciesHack = project.newTask(
                    "publish${variantName}BundleWrapper"
            ) {
                if (extension._artifactDir == null) {
                    dependsOn(processPackageMetadata)
                    // TODO https://issuetracker.google.com/issues/109918868
                    project.tasks.findByName(
                            (this@whenObjectAdded as InstallableVariantImpl).variantData
                                    .getTaskName("bundle", "")
                    )?.let {
                        dependsOn(it)
                    } ?: logger.warn("Bundle task not found, make sure to use " +
                                             "'com.android.tools.build:gradle' v3.2+. " +
                                             "Publishing App Bundles may not work.")
                }
            }
            val publishBundleTask = project.newTask<PublishBundle>(
                    "publish${variantName}Bundle",
                    "Uploads App Bundle for variant '$name'.",
                    arrayOf(extension, this)
            ) {
                resDir = playResourcesTask.get().resDir

                dependsOn(playResourcesTask)
                dependsOn(publishBundleTaskDependenciesHack)
            }
            publishBundleAllTask.configure { dependsOn(publishBundleTask) }

            val promoteReleaseTask = project.newTask<PromoteRelease>(
                    "promote${variantName}Artifact",
                    "Promotes a release for variant '$name'.",
                    arrayOf(extension, this)
            ) {
                resDir = playResourcesTask.get().resDir

                dependsOn(playResourcesTask)
            }
            promoteReleaseAllTask.configure { dependsOn(promoteReleaseTask) }

            val publishTask = project.newTask<LifecycleHelperTask>(
                    "publish$variantName",
                    "Uploads APK or App Bundle and all Play Store metadata for variant '$name'.",
                    arrayOf(extension)
            ) {
                dependsOn(if (extension.defaultToAppBundles) publishBundleTask else publishApkTask)
                dependsOn(publishListingTask)
                dependsOn(publishProductsTask)
            }
            publishAllTask.configure { dependsOn(publishTask) }
        }
    }
}
