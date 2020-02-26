package com.github.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.api.InstallableVariantImpl
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.common.validation.validateRuntime
import com.github.triplet.gradle.play.internal.PLAY_CONFIGS_PATH
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.PlayExtensionConfig
import com.github.triplet.gradle.play.internal.buildExtension
import com.github.triplet.gradle.play.internal.config
import com.github.triplet.gradle.play.internal.flavorNameOrDefault
import com.github.triplet.gradle.play.internal.getCommitEditTask
import com.github.triplet.gradle.play.internal.getGenEditTask
import com.github.triplet.gradle.play.internal.newTask
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.github.triplet.gradle.play.internal.validateCreds
import com.github.triplet.gradle.play.internal.validateDebuggability
import com.github.triplet.gradle.play.tasks.Bootstrap
import com.github.triplet.gradle.play.tasks.GenerateResources
import com.github.triplet.gradle.play.tasks.InstallInternalSharingArtifact
import com.github.triplet.gradle.play.tasks.ProcessArtifactMetadata
import com.github.triplet.gradle.play.tasks.PromoteRelease
import com.github.triplet.gradle.play.tasks.PublishApk
import com.github.triplet.gradle.play.tasks.PublishBundle
import com.github.triplet.gradle.play.tasks.PublishInternalSharingApk
import com.github.triplet.gradle.play.tasks.PublishInternalSharingBundle
import com.github.triplet.gradle.play.tasks.PublishListings
import com.github.triplet.gradle.play.tasks.PublishProducts
import com.github.triplet.gradle.play.tasks.internal.BootstrapLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.BootstrapOptions
import com.github.triplet.gradle.play.tasks.internal.GlobalPublishableArtifactLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.UpdatableTrackLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.WriteTrackLifecycleTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.container
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findPlugin
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import kotlin.reflect.KMutableProperty1

@Suppress("unused") // Used by Gradle
internal class PlayPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.validateRuntime()

        project.extensions.create<PlayPublisherExtension>(PLAY_PATH)
        project.plugins.withType<AppPlugin> {
            applyInternal(project)
        }
        project.afterEvaluate {
            if (project.plugins.findPlugin(AppPlugin::class) == null) {
                throw IllegalStateException(
                        "The Android Gradle Plugin was not applied. Gradle Play Publisher " +
                                "cannot be configured.")
            }
        }
    }

    private fun applyInternal(project: Project) {
        val baseExtension = project.extensions.getByType<PlayPublisherExtension>()
        val extensionContainer = project.container<PlayPublisherExtension>()
        val bootstrapOptionsHolder = BootstrapOptions.Holder()

        val bootstrapAllTask = project.newTask<BootstrapLifecycleTask>(
                "bootstrap",
                """
                |Downloads the Play Store listing metadata for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#quickstart
                """.trimMargin(),
                arrayOf(bootstrapOptionsHolder)
        )
        val publishAllTask = project.newTask<GlobalPublishableArtifactLifecycleTask>(
                "publish",
                """
                |Uploads APK or App Bundle and all Play Store metadata for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#managing-artifacts
                """.trimMargin(),
                arrayOf(baseExtension)
        )
        val publishApkAllTask = project.newTask<PublishableTrackLifecycleTask>(
                "publishApk",
                """
                |Uploads APK for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-apks
                """.trimMargin(),
                arrayOf(baseExtension)
        )
        val publishBundleAllTask = project.newTask<PublishableTrackLifecycleTask>(
                "publishBundle",
                """
                |Uploads App Bundle for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-an-app-bundle
                """.trimMargin(),
                arrayOf(baseExtension)
        )
        val promoteReleaseAllTask = project.newTask<UpdatableTrackLifecycleTask>(
                "promoteArtifact",
                """
                |Promotes a release for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#promoting-artifacts
                """.trimMargin(),
                arrayOf(baseExtension)
        )
        val publishListingAllTask = project.newTask<WriteTrackLifecycleTask>(
                "publishListing",
                """
                |Uploads all Play Store metadata for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-listings
                """.trimMargin(),
                arrayOf(baseExtension)
        )
        val publishProductsAllTask = project.newTask(
                "publishProducts",
                """
                |Uploads all Play Store in-app products for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-products
                """.trimMargin()
        )

        val android = project.the<AppExtension>()
        (android as ExtensionAware).extensions.add(PLAY_CONFIGS_PATH, extensionContainer)
        android.applicationVariants.whenObjectAdded {
            val variantName = name.capitalize()
            val extension = buildExtension(extensionContainer, baseExtension)
            project.logger.debug("Extension computed for variant '$name': $extension")

            if (!extension.isEnabled) {
                project.logger.info("Gradle Play Publisher is disabled for variant '$name'.")
                return@whenObjectAdded
            }
            extension.validateCreds()

            if (!isSigningReady && !outputsAreSigned) {
                project.logger.error(
                        "Signing not ready for Gradle Play Publisher. " +
                                "Be sure to specify a signingConfig for variant '$name'.")
            }


            val publishApkTaskDependenciesHack = project.newTask(
                    "publish${variantName}ApkWrapper"
            ) {
                val addDependencies = {
                    if (extension.config.artifactDir == null) {
                        assembleProvider?.let {
                            dependsOn(it)
                        } ?: logger.warn("Assemble task not found. Publishing APKs may not work.")
                    }
                }
                addDependencies()

                extension._callbacks += { property: KMutableProperty1<PlayExtensionConfig, Any?>,
                                          value: Any? ->
                    if (property.name == PlayExtensionConfig::artifactDir.name) {
                        setDependsOn(emptyList<Any>())
                        addDependencies()
                    }
                }
            }

            val publishBundleTaskDependenciesHack = project.newTask(
                    "publish${variantName}BundleWrapper"
            ) {
                val addDependencies = {
                    if (extension.config.artifactDir == null) {
                        // TODO blocked by https://issuetracker.google.com/issues/109918868
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
                addDependencies()

                extension._callbacks += { property: KMutableProperty1<PlayExtensionConfig, Any?>,
                                          value: Any? ->
                    if (property.name == PlayExtensionConfig::artifactDir.name) {
                        setDependsOn(emptyList<Any>())
                        addDependencies()
                    }
                }
            }

            val publishInternalSharingApkTask = project.newTask<PublishInternalSharingApk>(
                    "upload${variantName}PrivateApk",
                    """
                    |Uploads Internal Sharing APK for variant '$name'.
                    |   See https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact
                    """.trimMargin(),
                    arrayOf(extension, this)
            ) {
                outputDirectory.set(project.layout.buildDirectory.dir(
                        "outputs/internal-sharing/apk/${variant.name}"))

                dependsOn(publishApkTaskDependenciesHack)
            }

            val publishInternalSharingBundleTask = project.newTask<PublishInternalSharingBundle>(
                    "upload${variantName}PrivateBundle",
                    """
                    |Uploads Internal Sharing App Bundle for variant '$name'.
                    |   See https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact
                    """.trimMargin(),
                    arrayOf(extension, this)
            ) {
                outputDirectory.set(project.layout.buildDirectory.dir(
                        "outputs/internal-sharing/bundle/${variant.name}"))

                dependsOn(publishBundleTaskDependenciesHack)
            }

            project.newTask<InstallInternalSharingArtifact>(
                    "install${variantName}PrivateArtifact",
                    """
                    |Launches an intent to install an Internal Sharing artifact for variant '$name'.
                    |   See https://github.com/Triple-T/gradle-play-publisher#installing-internal-sharing-artifacts
                    """.trimMargin(),
                    arrayOf(android)
            ) {
                uploadedArtifacts.set(if (extension.defaultToAppBundles) {
                    publishInternalSharingBundleTask.flatMap { it.outputDirectory }
                } else {
                    publishInternalSharingApkTask.flatMap { it.outputDirectory }
                })
            }


            if (!validateDebuggability()) return@whenObjectAdded

            val genEditTask = project.getGenEditTask(applicationId, extension)
            val commitEditTask = project.getCommitEditTask(applicationId, extension)
            val editFile = genEditTask.flatMap { it.editIdFile }
            genEditTask { finalizedBy(commitEditTask) }

            val bootstrapTask = project.newTask<Bootstrap>(
                    "bootstrap$variantName",
                    """
                    |Downloads the Play Store listing metadata for variant '$name'.
                    |   See https://github.com/Triple-T/gradle-play-publisher#quickstart
                    """.trimMargin(),
                    arrayOf(extension, this, bootstrapOptionsHolder)
            ) {
                srcDir.set(project.file("src/${variant.flavorNameOrDefault}/$PLAY_PATH"))
                editIdFile.set(editFile)

                dependsOn(genEditTask)
            }
            bootstrapAllTask { dependsOn(bootstrapTask) }
            // TODO(#710): Remove in v3.0
            project.newTask("bootstrap${variantName}PlayResources") {
                dependsOn(bootstrapTask)
                doFirst { logger.warn("$name is deprecated, use ${bootstrapTask.get().name} instead") }
            }

            val resourceDir = project.newTask<GenerateResources>(
                    "generate${variantName}PlayResources"
            ) {
                val dirs = sourceSets.map {
                    project.layout.projectDirectory.dir("src/${it.name}/$PLAY_PATH")
                }
                resSrcDirs.set(dirs)
                resSrcTree.setFrom(dirs.map { project.fileTree(it).apply { exclude("**/.*") } })

                resDir.set(project.layout.buildDirectory.dir(playPath))

                mustRunAfter(bootstrapTask)
            }.flatMap {
                it.resDir
            }

            val publishListingTask = project.newTask<PublishListings>(
                    "publish${variantName}Listing",
                    """
                    |Uploads all Play Store metadata for variant '$name'.
                    |   See https://github.com/Triple-T/gradle-play-publisher#publishing-listings
                    """.trimMargin(),
                    arrayOf(extension, this)
            ) {
                resDir.set(resourceDir)
                editIdFile.set(editFile)

                dependsOn(genEditTask)
            }
            commitEditTask { mustRunAfter(publishListingTask) }
            publishListingAllTask { dependsOn(publishListingTask) }
            // TODO(#710): Remove in v3.0
            project.newTask("publishListing$variantName") {
                dependsOn(publishListingTask)
                doFirst { logger.warn("$name is deprecated, use ${publishListingTask.get().name} instead") }
            }

            val publishProductsTask = project.newTask<PublishProducts>(
                    "publish${variantName}Products",
                    """
                    |Uploads all Play Store in-app products for variant '$name'.
                    |   See https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-products
                    """.trimMargin(),
                    arrayOf(extension, this)
            ) {
                productsDir.setFrom(resourceDir.map {
                    it.dir(PRODUCTS_PATH).asFileTree.matching { include("*.json") }
                })
            }
            publishProductsAllTask { dependsOn(publishProductsTask) }

            val processArtifactMetadata = project.newTask<ProcessArtifactMetadata>(
                    "process${variantName}Metadata",
                    constructorArgs = arrayOf(extension, this)
            ) {
                editIdFile.set(editFile)

                val shouldRun =
                        extension.config.resolutionStrategyOrDefault == ResolutionStrategy.AUTO
                onlyIf { shouldRun }
                if (shouldRun) dependsOn(genEditTask)
            }
            preBuildProvider { dependsOn(processArtifactMetadata) }
            publishApkTaskDependenciesHack { dependsOn(processArtifactMetadata) }
            publishBundleTaskDependenciesHack { dependsOn(processArtifactMetadata) }

            val publishApkTask = project.newTask<PublishApk>(
                    "publish${variantName}Apk",
                    """
                    |Uploads APK for variant '$name'.
                    |   See https://github.com/Triple-T/gradle-play-publisher#publishing-apks
                    """.trimMargin(),
                    arrayOf(extension, this)
            ) {
                resDir.set(resourceDir)
                editIdFile.set(editFile)

                dependsOn(resourceDir)
                dependsOn(genEditTask)
                dependsOn(publishApkTaskDependenciesHack)
            }
            commitEditTask { mustRunAfter(publishApkTask) }
            publishApkAllTask { dependsOn(publishApkTask) }
            // TODO(#710): Remove in v3.0
            project.newTask("publishApk$variantName") {
                dependsOn(publishApkTask)
                doFirst { logger.warn("$name is deprecated, use ${publishApkTask.get().name} instead") }
            }

            val publishBundleTask = project.newTask<PublishBundle>(
                    "publish${variantName}Bundle",
                    """
                    |Uploads App Bundle for variant '$name'.
                    |   See https://github.com/Triple-T/gradle-play-publisher#publishing-an-app-bundle
                    """.trimMargin(),
                    arrayOf(extension, this)
            ) {
                resDir.set(resourceDir)
                editIdFile.set(editFile)

                dependsOn(resourceDir)
                dependsOn(genEditTask)
                dependsOn(publishBundleTaskDependenciesHack)
            }
            commitEditTask { mustRunAfter(publishBundleTask) }
            publishBundleAllTask { dependsOn(publishBundleTask) }

            val promoteReleaseTask = project.newTask<PromoteRelease>(
                    "promote${variantName}Artifact",
                    """
                    |Promotes a release for variant '$name'.
                    |   See https://github.com/Triple-T/gradle-play-publisher#promoting-artifacts
                    """.trimMargin(),
                    arrayOf(extension, this)
            ) {
                resDir.set(resourceDir)
                editIdFile.set(editFile)

                dependsOn(resourceDir)
                dependsOn(genEditTask)

                mustRunAfter(publishApkTask)
                mustRunAfter(publishBundleTask)
            }
            commitEditTask { mustRunAfter(promoteReleaseTask) }
            promoteReleaseAllTask { dependsOn(promoteReleaseTask) }

            val publishTask = project.newTask<GlobalPublishableArtifactLifecycleTask>(
                    "publish$variantName",
                    """
                    |Uploads APK or App Bundle and all Play Store metadata for variant '$name'.
                    |   See https://github.com/Triple-T/gradle-play-publisher#managing-artifacts
                    """.trimMargin(),
                    arrayOf(extension)
            ) {
                dependsOn(if (extension.defaultToAppBundles) publishBundleTask else publishApkTask)
                dependsOn(publishListingTask)
                dependsOn(publishProductsTask)
            }
            publishAllTask { dependsOn(publishTask) }
        }

        project.afterEvaluate {
            val allPossiblePlayConfigNames: Set<String> by lazy {
                android.applicationVariants.flatMapTo(mutableSetOf()) {
                    listOf(it.name, it.buildType.name) + it.productFlavors.map { it.name }
                }
            }

            for (configName in extensionContainer.names) {
                if (configName !in allPossiblePlayConfigNames) {
                    project.logger.warn(
                            "Warning: Gradle Play Publisher playConfigs object '$configName' " +
                                    "does not match a variant, flavor, or build type.")
                }
            }
        }
    }
}
