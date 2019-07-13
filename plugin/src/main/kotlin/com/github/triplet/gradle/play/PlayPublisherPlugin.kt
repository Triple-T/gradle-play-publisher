package com.github.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.api.InstallableVariantImpl
import com.github.triplet.gradle.play.internal.PLAY_CONFIGS_PATH
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.mergeWith
import com.github.triplet.gradle.play.internal.newTask
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.validateRuntime
import com.github.triplet.gradle.play.tasks.Bootstrap
import com.github.triplet.gradle.play.tasks.GenerateResources
import com.github.triplet.gradle.play.tasks.ProcessArtifactMetadata
import com.github.triplet.gradle.play.tasks.PromoteRelease
import com.github.triplet.gradle.play.tasks.PublishApk
import com.github.triplet.gradle.play.tasks.PublishBundle
import com.github.triplet.gradle.play.tasks.PublishInternalSharingApk
import com.github.triplet.gradle.play.tasks.PublishInternalSharingBundle
import com.github.triplet.gradle.play.tasks.PublishListing
import com.github.triplet.gradle.play.tasks.PublishProducts
import com.github.triplet.gradle.play.tasks.internal.BootstrapLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.BootstrapOptions
import com.github.triplet.gradle.play.tasks.internal.GlobalPublishableArtifactLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.TransientTrackOptions
import com.github.triplet.gradle.play.tasks.internal.UpdatableTrackLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.WriteTrackLifecycleTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.container
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import java.io.File

@Suppress("unused") // Used by Gradle
class PlayPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        validateRuntime()

        project.plugins.withType<AppPlugin> {
            applyInternal(project)
        }
    }

    private fun applyInternal(project: Project) {
        val baseExtension = project.extensions.create<PlayPublisherExtension>(PLAY_PATH)
        val extensionContainer = project.container<PlayPublisherExtension>()
        val bootstrapOptionsHolder = BootstrapOptions.Holder()
        val transientTrackOptionsHolder = TransientTrackOptions.Holder()

        val bootstrapAllTask = project.newTask<BootstrapLifecycleTask>(
                "bootstrap",
                "Downloads the Play Store listing metadata for all variants. See " +
                        "https://github.com/Triple-T/gradle-play-publisher#quickstart",
                arrayOf(bootstrapOptionsHolder)
        )
        val publishAllTask = project.newTask<GlobalPublishableArtifactLifecycleTask>(
                "publish",
                "Uploads APK or App Bundle and all Play Store metadata for every variant. See " +
                        "https://github.com/Triple-T/gradle-play-publisher#managing-artifacts",
                arrayOf(baseExtension)
        )
        val publishApkAllTask = project.newTask<PublishableTrackLifecycleTask>(
                "publishApk",
                "Uploads APK for every variant. See " +
                        "https://github.com/Triple-T/gradle-play-publisher#publishing-apks",
                arrayOf(baseExtension, transientTrackOptionsHolder)
        )
        val publishBundleAllTask = project.newTask<PublishableTrackLifecycleTask>(
                "publishBundle",
                "Uploads App Bundle for every variant. See " +
                        "https://github.com/Triple-T/gradle-play-publisher#publishing-an-app-bundle",
                arrayOf(baseExtension, transientTrackOptionsHolder)
        )
        val promoteReleaseAllTask = project.newTask<UpdatableTrackLifecycleTask>(
                "promoteArtifact",
                "Promotes a release for every variant. See " +
                        "https://github.com/Triple-T/gradle-play-publisher#promoting-artifacts",
                arrayOf(baseExtension, transientTrackOptionsHolder)
        )
        val publishListingAllTask = project.newTask<WriteTrackLifecycleTask>(
                "publishListing",
                "Uploads all Play Store metadata for every variant. See " +
                        "https://github.com/Triple-T/gradle-play-publisher#publishing-listings",
                arrayOf(baseExtension)
        )
        val publishProductsAllTask = project.newTask(
                "publishProducts",
                "Uploads all Play Store in-app products for every variant. See " +
                        "https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-products"
        )

        val android = project.the<AppExtension>()
        (android as ExtensionAware).extensions.add(PLAY_CONFIGS_PATH, extensionContainer)
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
                    "bootstrap$variantName",
                    "Downloads the Play Store listing metadata for variant '$name'. See " +
                            "https://github.com/Triple-T/gradle-play-publisher#quickstart",
                    arrayOf(extension, this, bootstrapOptionsHolder)
            )
            bootstrapAllTask { dependsOn(bootstrapTask) }
            // TODO Remove in v3.0
            project.newTask("bootstrap${variantName}PlayResources") {
                dependsOn(bootstrapTask)
                doFirst { logger.warn("$name is deprecated, use ${bootstrapTask.get().name} instead") }
            }

            val resourceDir = project.newTask<GenerateResources>(
                    "generate${variantName}PlayResources",
                    constructorArgs = arrayOf(this)
            ) {
                resDir.set(File(project.buildDir, "$playPath/res"))
                mustRunAfter(bootstrapTask)
            }.flatMap {
                it.resDir
            }

            val publishListingTask = project.newTask<PublishListing>(
                    "publish${variantName}Listing",
                    "Uploads all Play Store metadata for variant '$name'. See " +
                            "https://github.com/Triple-T/gradle-play-publisher#publishing-listings",
                    arrayOf(extension, this)
            ) { resDir.set(resourceDir) }
            publishListingAllTask { dependsOn(publishListingTask) }
            // TODO Remove in v3.0
            project.newTask("publishListing$variantName") {
                dependsOn(publishListingTask)
                doFirst { logger.warn("$name is deprecated, use ${publishListingTask.get().name} instead") }
            }

            val publishProductsTask = project.newTask<PublishProducts>(
                    "publish${variantName}Products",
                    "Uploads all Play Store in-app products for variant '$name'. See " +
                            "https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-products",
                    arrayOf(extension, this)
            ) { resDir.set(resourceDir) }
            publishProductsAllTask { dependsOn(publishProductsTask) }

            val processArtifactMetadata = project.newTask<ProcessArtifactMetadata>(
                    "process${variantName}Metadata",
                    constructorArgs = arrayOf(extension, this)
            )
            checkManifestProvider { dependsOn(processArtifactMetadata) }
            generateBuildConfigProvider { dependsOn(processArtifactMetadata) }

            val publishApkTaskDependenciesHack = project.newTask(
                    "publish${variantName}ApkWrapper"
            ) {
                if (extension._artifactDir == null) {
                    dependsOn(processArtifactMetadata)
                    assembleProvider?.let {
                        dependsOn(it)
                    } ?: logger.warn("Assemble task not found. Publishing APKs may not work.")
                }
            }
            val publishApkTask = project.newTask<PublishApk>(
                    "publish${variantName}Apk",
                    "Uploads APK for variant '$name'. See " +
                            "https://github.com/Triple-T/gradle-play-publisher#publishing-apks",
                    arrayOf(extension, this, transientTrackOptionsHolder)
            ) {
                resDir.set(resourceDir)

                dependsOn(resourceDir)
                dependsOn(publishApkTaskDependenciesHack)
            }
            publishApkAllTask { dependsOn(publishApkTask) }
            // TODO Remove in v3.0
            project.newTask("publishApk$variantName") {
                dependsOn(publishApkTask)
                doFirst { logger.warn("$name is deprecated, use ${publishApkTask.get().name} instead") }
            }

            project.newTask<PublishInternalSharingApk>(
                    "upload${variantName}PrivateApk",
                    "Uploads Internal Sharing APK for variant '$name'. See " +
                            "https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact",
                    arrayOf(extension, this)
            ) { dependsOn(publishApkTaskDependenciesHack) }

            val publishBundleTaskDependenciesHack = project.newTask(
                    "publish${variantName}BundleWrapper"
            ) {
                if (extension._artifactDir == null) {
                    dependsOn(processArtifactMetadata)
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
                    "Uploads App Bundle for variant '$name'. See " +
                            "https://github.com/Triple-T/gradle-play-publisher#publishing-an-app-bundle",
                    arrayOf(extension, this, transientTrackOptionsHolder)
            ) {
                resDir.set(resourceDir)

                dependsOn(resourceDir)
                dependsOn(publishBundleTaskDependenciesHack)
            }
            publishBundleAllTask { dependsOn(publishBundleTask) }

            project.newTask<PublishInternalSharingBundle>(
                    "upload${variantName}PrivateBundle",
                    "Uploads Internal Sharing App Bundle for variant '$name'. See " +
                            "https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact",
                    arrayOf(extension, this)
            ) { dependsOn(publishBundleTaskDependenciesHack) }

            val promoteReleaseTask = project.newTask<PromoteRelease>(
                    "promote${variantName}Artifact",
                    "Promotes a release for variant '$name'. See " +
                            "https://github.com/Triple-T/gradle-play-publisher#promoting-artifacts",
                    arrayOf(extension, this, transientTrackOptionsHolder)
            ) {
                resDir.set(resourceDir)

                dependsOn(resourceDir)
            }
            promoteReleaseAllTask { dependsOn(promoteReleaseTask) }

            val publishTask = project.newTask<GlobalPublishableArtifactLifecycleTask>(
                    "publish$variantName",
                    "Uploads APK or App Bundle and all Play Store metadata for variant '$name'. See " +
                            "https://github.com/Triple-T/gradle-play-publisher#managing-artifacts",
                    arrayOf(extension)
            ) {
                dependsOn(if (extension.defaultToAppBundles) publishBundleTask else publishApkTask)
                dependsOn(publishListingTask)
                dependsOn(publishProductsTask)
            }
            publishAllTask { dependsOn(publishTask) }
        }
    }
}
