package com.github.triplet.gradle.play

import com.android.build.api.artifact.ArtifactType
import com.android.build.api.variant.impl.ApplicationVariantPropertiesImpl
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.common.utils.safeMkdirs
import com.github.triplet.gradle.common.validation.validateRuntime
import com.github.triplet.gradle.play.internal.CliPlayPublisherExtension
import com.github.triplet.gradle.play.internal.INTERMEDIATES_OUTPUT_PATH
import com.github.triplet.gradle.play.internal.PLAY_CONFIGS_PATH
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import com.github.triplet.gradle.play.internal.buildExtension
import com.github.triplet.gradle.play.internal.flavorNameOrDefault
import com.github.triplet.gradle.play.internal.getCommitEditTask
import com.github.triplet.gradle.play.internal.getGenEditTask
import com.github.triplet.gradle.play.internal.newTask
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.toConfig
import com.github.triplet.gradle.play.internal.validateCreds
import com.github.triplet.gradle.play.internal.validateDebuggability
import com.github.triplet.gradle.play.tasks.Bootstrap
import com.github.triplet.gradle.play.tasks.GenerateResources
import com.github.triplet.gradle.play.tasks.InstallInternalSharingArtifact
import com.github.triplet.gradle.play.tasks.ProcessArtifactVersionCodes
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
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.UpdatableTrackLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.WriteTrackLifecycleTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.kotlin.dsl.container
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findPlugin
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

@Suppress("unused") // Used by Gradle
internal class PlayPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.validateRuntime()

        project.extensions.create<PlayPublisherExtension>(PLAY_PATH).apply {
            enabled.convention(true)
            defaultToAppBundles.convention(false)
            commit.convention(true)
            track.convention("internal")
            resolutionStrategy.convention(ResolutionStrategy.FAIL)
        }

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
        val cliOptionsExtension = project.objects.newInstance<CliPlayPublisherExtension>()
        val bootstrapOptionsHolder = BootstrapOptions.Holder()

        val bootstrapAllTask = project.newTask<BootstrapLifecycleTask>(
                "bootstrap",
                """
                |Downloads the Play Store listing metadata for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#quickstart
                """.trimMargin(),
                arrayOf(bootstrapOptionsHolder)
        )
        val publishAllTask = project.newTask("publishApps")
        val publishApkAllTask = project.newTask<PublishableTrackLifecycleTask>(
                "publishApk",
                """
                |Uploads APK for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-apks
                """.trimMargin(),
                arrayOf(cliOptionsExtension)
        )
        val publishBundleAllTask = project.newTask<PublishableTrackLifecycleTask>(
                "publishBundle",
                """
                |Uploads App Bundle for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-an-app-bundle
                """.trimMargin(),
                arrayOf(cliOptionsExtension)
        )
        val promoteReleaseAllTask = project.newTask<UpdatableTrackLifecycleTask>(
                "promoteArtifact",
                """
                |Promotes a release for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#promoting-artifacts
                """.trimMargin(),
                arrayOf(cliOptionsExtension)
        )
        val publishListingAllTask = project.newTask<WriteTrackLifecycleTask>(
                "publishListing",
                """
                |Uploads all Play Store metadata for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-listings
                """.trimMargin(),
                arrayOf(cliOptionsExtension)
        )
        val publishProductsAllTask = project.newTask(
                "publishProducts",
                """
                |Uploads all Play Store in-app products for every variant.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-products
                """.trimMargin()
        )

        project.afterEvaluate {
            if (project.plugins.findPlugin(PublishingPlugin::class) == null) {
                project.newTask<GlobalPublishableArtifactLifecycleTask>(
                        "publish",
                        """
                        |Uploads APK or App Bundle and all Play Store metadata for every variant.
                        |   See https://github.com/Triple-T/gradle-play-publisher#managing-artifacts
                        """.trimMargin(),
                        arrayOf(cliOptionsExtension)
                ) {
                    dependsOn(publishAllTask)
                }
            } else {
                project.tasks.named("publish") { dependsOn(publishAllTask) }
            }
        }

        val baseExtension = project.extensions.getByType<PlayPublisherExtension>()
        val extensionContainer = project.container<PlayPublisherExtension>()
        val android = project.the<BaseAppModuleExtension>()
        (android as ExtensionAware).extensions.add(PLAY_CONFIGS_PATH, extensionContainer)

        android.onVariants v@{
            val variantName = name.capitalize()
            val extension = buildExtension(extensionContainer, baseExtension, cliOptionsExtension)
            project.logger.debug("Extension computed for variant '$name': ${extension.toConfig()}")

            if (!extension.enabled.get()) {
                project.logger.info("Gradle Play Publisher is disabled for variant '$name'.")
                return@v
            }
            extension.validateCreds()

            onProperties p@{
                fun findApkFiles(): Provider<List<String>> = extension.artifactDir.map {
                    val customDir = it.asFile
                    if (customDir.isFile && customDir.extension == "apk") {
                        listOf(it.asFile.absolutePath)
                    } else {
                        it.asFileTree.matching {
                            include("*.apk")
                        }.map { it.absolutePath }
                    }
                }.orElse(artifacts.get(ArtifactType.APK).map {
                    artifacts.getBuiltArtifactsLoader().load(it)
                            ?.elements?.map { it.outputFile }.sneakyNull()
                })

                fun findBundleFile(
                ): Provider<RegularFile> = extension.artifactDir.map { customDir ->
                    if (customDir.asFile.isFile && customDir.asFile.extension == "aab") {
                        customDir.file(".")
                    } else {
                        customDir.asFileTree.matching {
                            include("*.aab")
                        }.singleOrNull()?.let {
                            customDir.file(it.toString())
                        } ?: customDir.file("ERROR_no-unique-aab-found")
                    }
                }.orElse(artifacts.get(ArtifactType.BUNDLE))

                fun findDeobfuscationFile(
                ): Provider<RegularFile> = extension.artifactDir.map { customDir ->
                    customDir.asFileTree.matching {
                        include("mapping.txt")
                    }.singleOrNull()?.let { customDir.file(it.absolutePath) }.sneakyNull()
                }.orElse(project.provider {
                    artifacts.get(ArtifactType.OBFUSCATION_MAPPING_FILE).takeUnless {
                        extension.artifactDir.isPresent
                    }
                }.flatMap { it })

                val appId = applicationId.get()

                val publishInternalSharingApkTask = project.newTask<PublishInternalSharingApk>(
                        "upload${variantName}PrivateApk",
                        """
                        |Uploads Internal Sharing APK for variant '$name'.
                        |   See https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact
                        """.trimMargin(),
                        arrayOf(extension, appId)
                ) {
                    apks.from(findApkFiles())
                    outputDirectory.set(project.layout.buildDirectory.dir(
                            "outputs/internal-sharing/apk/${this@v.name}"))
                }

                val publishInternalSharingBundleTask = project.newTask<PublishInternalSharingBundle>(
                        "upload${variantName}PrivateBundle",
                        """
                        |Uploads Internal Sharing App Bundle for variant '$name'.
                        |   See https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact
                        """.trimMargin(),
                        arrayOf(extension, appId)
                ) {
                    bundle.set(findBundleFile())
                    outputDirectory.set(project.layout.buildDirectory.dir(
                            "outputs/internal-sharing/bundle/${this@v.name}"))
                }

                project.newTask<InstallInternalSharingArtifact>(
                        "install${variantName}PrivateArtifact",
                        """
                        |Launches an intent to install an Internal Sharing artifact for variant '$name'.
                        |   See https://github.com/Triple-T/gradle-play-publisher#installing-internal-sharing-artifacts
                        """.trimMargin(),
                        arrayOf(android)
                ) {
                    uploadedArtifacts.set(if (extension.defaultToAppBundles.get()) {
                        publishInternalSharingBundleTask.flatMap { it.outputDirectory }
                    } else {
                        publishInternalSharingApkTask.flatMap { it.outputDirectory }
                    })
                }


                if (!validateDebuggability()) return@p

                val genEditTask = project.getGenEditTask(appId, extension)
                val commitEditTask = project.getCommitEditTask(appId, extension)
                val editFile = genEditTask.flatMap { it.editIdFile }
                genEditTask { finalizedBy(commitEditTask) }

                val bootstrapTask = project.newTask<Bootstrap>(
                        "bootstrap$variantName",
                        """
                        |Downloads the Play Store listing metadata for variant '$name'.
                        |   See https://github.com/Triple-T/gradle-play-publisher#quickstart
                        """.trimMargin(),
                        arrayOf(extension, appId, bootstrapOptionsHolder)
                ) {
                    srcDir.set(project.file("src/$flavorNameOrDefault/$PLAY_PATH"))
                    editIdFile.set(editFile)

                    dependsOn(genEditTask)
                }
                bootstrapAllTask { dependsOn(bootstrapTask) }

                // TODO(asaveau): remove internal API usage
                val sourceSets = run {
                    (this as ApplicationVariantPropertiesImpl).variantSources.sortedSourceProviders
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
                        arrayOf(extension, appId)
                ) {
                    resDir.set(resourceDir)
                    editIdFile.set(editFile)

                    dependsOn(genEditTask)
                }
                commitEditTask { mustRunAfter(publishListingTask) }
                publishListingAllTask { dependsOn(publishListingTask) }

                val publishProductsTask = project.newTask<PublishProducts>(
                        "publish${variantName}Products",
                        """
                        |Uploads all Play Store in-app products for variant '$name'.
                        |   See https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-products
                        """.trimMargin(),
                        arrayOf(extension, appId)
                ) {
                    productsDir.setFrom(resourceDir.map {
                        it.dir(PRODUCTS_PATH).asFileTree.matching { include("*.json") }
                    })
                }
                publishProductsAllTask { dependsOn(publishProductsTask) }

                val staticVersionCodes = outputs.map { it.versionCode.get() }
                val processArtifactVersionCodes = project.newTask<ProcessArtifactVersionCodes>(
                        "process${variantName}VersionCodes",
                        constructorArgs = arrayOf(extension, appId)
                ) {
                    editIdFile.set(editFile)
                    versionCodes.set(staticVersionCodes)
                    playVersionCodes.set(project.layout.buildDirectory.file(
                            "$INTERMEDIATES_OUTPUT_PATH/${this@v.name}/available-version-codes.txt"))

                    dependsOn(genEditTask)
                }
                if (extension.resolutionStrategy.get() == ResolutionStrategy.AUTO) {
                    for ((i, output) in outputs.withIndex()) {
                        output.versionCode.set(processArtifactVersionCodes.map {
                            it.playVersionCodes.get().asFile.readLines()[i].toInt()
                        })
                    }
                }


                fun PublishArtifactTaskBase.configureInputs() {
                    editIdFile.set(editFile)
                    releaseNotesDir.set(resourceDir.map {
                        it.dir(RELEASE_NOTES_PATH).also { it.asFile.safeMkdirs() }
                    })
                    consoleNamesDir.set(resourceDir.map {
                        it.dir(RELEASE_NAMES_PATH).also { it.asFile.safeMkdirs() }
                    })
                }

                val publishApkTask = project.newTask<PublishApk>(
                        "publish${variantName}Apk",
                        """
                        |Uploads APK for variant '$name'.
                        |   See https://github.com/Triple-T/gradle-play-publisher#publishing-apks
                        """.trimMargin(),
                        arrayOf(extension, appId)
                ) {
                    configureInputs()
                    apks.from(findApkFiles())
                    mappingFile.set(findDeobfuscationFile())

                    dependsOn(genEditTask)
                }
                commitEditTask { mustRunAfter(publishApkTask) }
                publishApkAllTask { dependsOn(publishApkTask) }

                val publishBundleTask = project.newTask<PublishBundle>(
                        "publish${variantName}Bundle",
                        """
                        |Uploads App Bundle for variant '$name'.
                        |   See https://github.com/Triple-T/gradle-play-publisher#publishing-an-app-bundle
                        """.trimMargin(),
                        arrayOf(extension, appId)
                ) {
                    configureInputs()
                    bundle.set(findBundleFile())
                    mappingFile.set(findDeobfuscationFile())

                    dependsOn(genEditTask)
                }
                commitEditTask { mustRunAfter(publishBundleTask) }
                publishBundleAllTask { dependsOn(publishBundleTask) }

                val promoteReleaseTask = project.newTask<PromoteRelease>(
                        "promote${variantName}Artifact",
                        """
                        |Promotes a release for variant '$name'.
                        |   See https://github.com/Triple-T/gradle-play-publisher#promoting-artifacts
                        """.trimMargin(),
                        arrayOf(extension, appId)
                ) {
                    configureInputs()

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
                    dependsOn(if (extension.defaultToAppBundles.get()) {
                        publishBundleTask
                    } else {
                        publishApkTask
                    })
                    dependsOn(publishListingTask)
                    dependsOn(publishProductsTask)
                }
                publishAllTask { dependsOn(publishTask) }
            }
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

    // TODO(asaveau): remove after https://github.com/gradle/gradle/issues/12388
    @Suppress("UNCHECKED_CAST")
    private fun <T> T?.sneakyNull() = this as T
}
