package com.github.triplet.gradle.play

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.common.utils.safeMkdirs
import com.github.triplet.gradle.common.validation.validateRuntime
import com.github.triplet.gradle.play.internal.CliPlayPublisherExtension
import com.github.triplet.gradle.play.internal.INTERMEDIATES_OUTPUT_PATH
import com.github.triplet.gradle.play.internal.OUTPUT_PATH
import com.github.triplet.gradle.play.internal.PLAY_CONFIGS_PATH
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import com.github.triplet.gradle.play.internal.SUBSCRIPTIONS_PATH
import com.github.triplet.gradle.play.internal.buildExtension
import com.github.triplet.gradle.play.internal.flavorNameOrDefault
import com.github.triplet.gradle.play.internal.generateExtensionOverrideOrdering
import com.github.triplet.gradle.play.internal.getCommitEditTask
import com.github.triplet.gradle.play.internal.newTask
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.toConfig
import com.github.triplet.gradle.play.internal.toPriority
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
import com.github.triplet.gradle.play.tasks.PublishSubscriptions
import com.github.triplet.gradle.play.tasks.internal.BootstrapLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.BootstrapOptions
import com.github.triplet.gradle.play.tasks.internal.GlobalPublishableArtifactLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.GlobalUploadableArtifactLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.PlayApiService
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.UpdatableTrackLifecycleTask
import com.github.triplet.gradle.play.tasks.internal.WriteTrackLifecycleTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceRegistration
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.container
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findPlugin
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType
import javax.inject.Inject

@Suppress("unused") // Used by Gradle
internal abstract class PlayPublisherPlugin @Inject constructor(
        private val buildEventsListenerRegistry: BuildEventsListenerRegistry,
) : Plugin<Project> {
    override fun apply(project: Project) {
        project.validateRuntime()

        project.extensions.create<PlayPublisherExtension>(PLAY_PATH, "default").apply {
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
        val executionDir = project.objects.directoryProperty()
                .apply { set(project.rootDir) }
                .get()
        val cliOptionsExtension = project.objects.newInstance<CliPlayPublisherExtension>()
        val bootstrapOptionsHolder = BootstrapOptions.Holder()

        // ---------------------------------- START: GLOBAL TASKS ----------------------------------

        val bootstrapAllTask = project.newTask<BootstrapLifecycleTask>(
                "bootstrapListing",
                """
                |Downloads the Play Store listing metadata for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#quickstart
                """.trimMargin(),
                arrayOf(bootstrapOptionsHolder),
        )
        val publishAllTask = project.newTask<GlobalPublishableArtifactLifecycleTask>(
                "publishApps",
                """
                |Uploads APK or App Bundle and all Play Store metadata for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#managing-artifacts
                """.trimMargin(),
                arrayOf(cliOptionsExtension, executionDir),
        )
        val publishApkAllTask = project.newTask<PublishableTrackLifecycleTask>(
                "publishApk",
                """
                |Uploads APK for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-apks
                """.trimMargin(),
                arrayOf(cliOptionsExtension, executionDir),
        )
        val publishBundleAllTask = project.newTask<PublishableTrackLifecycleTask>(
                "publishBundle",
                """
                |Uploads App Bundle for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-an-app-bundle
                """.trimMargin(),
                arrayOf(cliOptionsExtension, executionDir),
        )
        val publishInternalSharingApkAllTask = project.newTask<GlobalUploadableArtifactLifecycleTask>(
                "uploadPrivateApk",
                """
                |Uploads Internal Sharing APK for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact
                """.trimMargin(),
                arrayOf(cliOptionsExtension, executionDir),
        )
        val publishInternalSharingBundleAllTask = project.newTask<GlobalUploadableArtifactLifecycleTask>(
                "uploadPrivateBundle",
                """
                |Uploads Internal Sharing App Bundle for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact
                """.trimMargin(),
                arrayOf(cliOptionsExtension, executionDir),
        )
        val promoteReleaseAllTask = project.newTask<UpdatableTrackLifecycleTask>(
                "promoteArtifact",
                """
                |Promotes a release for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#promoting-artifacts
                """.trimMargin(),
                arrayOf(cliOptionsExtension, executionDir),
        )
        val publishListingAllTask = project.newTask<WriteTrackLifecycleTask>(
                "publishListing",
                """
                |Uploads all Play Store metadata for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-listings
                """.trimMargin(),
                arrayOf(cliOptionsExtension, executionDir),
        )
        val publishProductsAllTask = project.newTask<Task>(
                "publishProducts",
                """
                |Uploads all Play Store in-app products for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-products
                """.trimMargin(),
        )
        val publishSubscriptionsAllTask = project.newTask<Task>(
                "publishSubscriptions",
                """
                |Uploads all Play Store in-app subscriptions for all variants.
                |   See https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-subscriptions
                """.trimMargin(),
        )

        // ----------------------------------- END: GLOBAL TASKS -----------------------------------

        val baseExtension = project.extensions.getByType<PlayPublisherExtension>()
        val extensionContainer = project.container<PlayPublisherExtension>()
        val android = project.extensions.getByType<BaseAppModuleExtension>()
        (android as ExtensionAware).extensions.add(PLAY_CONFIGS_PATH, extensionContainer)

        val androidExtension = project.extensions.getByType<ApplicationAndroidComponentsExtension>()
        androidExtension.onVariants(androidExtension.selector().all()) v@{ variant ->
            val taskVariantName = variant.name.capitalize()
            val extensionStore = variant.buildExtension(
                    project,
                    extensionContainer,
                    baseExtension,
                    cliOptionsExtension,
            )
            val extension = extensionStore.getValue(variant.name)

            project.logger.debug(
                    "Extension computed for variant $taskVariantName: ${extension.toConfig()}")
            project.afterEvaluate {
                project.logger.debug(
                        "Extension resolved for variant $taskVariantName: ${extension.toConfig()}")
            }

            if (!extension.enabled.get()) {
                project.logger.info(
                        "Gradle Play Publisher is disabled for variant $taskVariantName.")
                return@v
            }

            fun findApkFiles(): Provider<List<String>> = extension.artifactDir.map {
                val customDir = it.asFile
                if (customDir.isFile && customDir.extension == "apk") {
                    listOf(it.asFile.absolutePath)
                } else {
                    it.asFileTree.matching {
                        include("*.apk")
                    }.map { it.absolutePath }
                }
            }.orElse(variant.artifacts.get(SingleArtifact.APK).map {
                variant.artifacts.getBuiltArtifactsLoader().load(it)
                        ?.elements?.map { it.outputFile }.sneakyNull()
            })

            fun findBundleFiles(): Provider<List<String>> = extension.artifactDir.map {
                val customDir = it.asFile
                if (customDir.isFile && customDir.extension == "aab") {
                    listOf(it.asFile.absolutePath)
                } else {
                    it.asFileTree.matching {
                        include("*.aab")
                    }.map { it.absolutePath }
                }
            }.orElse(variant.artifacts.get(SingleArtifact.BUNDLE).map {
                listOf(it.asFile.absolutePath)
            })

            @Suppress("UNCHECKED_CAST") // Needed for overload ambiguity
            fun getArtifactDependenciesHack(
                    artifact: SingleArtifact<*>,
            ): Provider<*> = extension.artifactDir.map<String> {
                ""
            }.orElse(variant.artifacts.get(artifact) as Provider<String>)

            val appId = variant.applicationId.get()
            val api = project.gradle.sharedServices.registerIfAbsent(
                    "playApi-$appId", PlayApiService::class) {
                parameters.appId.set(appId)
                parameters.editIdFile.set(project.layout.buildDirectory.file("$OUTPUT_PATH/$appId.txt"))
            }
            buildEventsListenerRegistry.onTaskCompletion(api)

            project.gradle.sharedServices.registrations.named<
                    BuildServiceRegistration<PlayApiService, PlayApiService.Params>
                    >("playApi-$appId") {
                val priorityProp = parameters._extensionPriority
                val newPriority = extension.toPriority()

                if (!priorityProp.isPresent || newPriority < priorityProp.get()) {
                    parameters.credentials.set(extension.serviceAccountCredentials)
                    parameters.useApplicationDefaultCredentials.set(extension.useApplicationDefaultCredentials)
                    parameters.impersonateServiceAccount.set(extension.impersonateServiceAccount)
                    priorityProp.set(newPriority)
                }
            }

            fun PublishTaskBase.bindApi(api: Provider<PlayApiService>) {
                usesService(api)
                apiService.value(api).disallowChanges()
            }

            // ------------------- START: ALL BUILD TYPE VARIANT SPECIFIC TASKS -------------------

            val publishInternalSharingApkTask = project.newTask<PublishInternalSharingApk>(
                    "upload${taskVariantName}PrivateApk",
                    """
                    |Uploads Internal Sharing APK for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact
                    """.trimMargin(),
                    arrayOf(extension, executionDir),
            ) {
                bindApi(api)
                apks.from(findApkFiles())
                outputDirectory.set(project.layout.buildDirectory.dir(
                        "outputs/internal-sharing/apk/${variant.name}"))

                dependsOn(getArtifactDependenciesHack(SingleArtifact.APK))
                configure3pDeps(extension, taskVariantName)
            }
            publishInternalSharingApkAllTask { dependsOn(publishInternalSharingApkTask) }

            val publishInternalSharingBundleTask = project.newTask<PublishInternalSharingBundle>(
                    "upload${taskVariantName}PrivateBundle",
                    """
                    |Uploads Internal Sharing App Bundle for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact
                    """.trimMargin(),
                    arrayOf(extension, executionDir),
            ) {
                bindApi(api)
                bundles.from(findBundleFiles())
                outputDirectory.set(project.layout.buildDirectory.dir(
                        "outputs/internal-sharing/bundle/${variant.name}"))

                dependsOn(getArtifactDependenciesHack(SingleArtifact.BUNDLE))
                configure3pDeps(extension, taskVariantName)
            }
            publishInternalSharingBundleAllTask { dependsOn(publishInternalSharingBundleTask) }

            project.newTask<InstallInternalSharingArtifact>(
                    "install${taskVariantName}PrivateArtifact",
                    """
                    |Launches an intent to install an Internal Sharing artifact for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#installing-internal-sharing-artifacts
                    """.trimMargin(),
                    arrayOf(android),
            ) {
                uploadedArtifacts.set(if (extension.defaultToAppBundles.get()) {
                    publishInternalSharingBundleTask.flatMap { it.outputDirectory }
                } else {
                    publishInternalSharingApkTask.flatMap { it.outputDirectory }
                })
            }

            // -------------------- END: ALL BUILD TYPE VARIANT SPECIFIC TASKS --------------------

            if (!variant.validateDebuggability()) return@v

            val commitEditTask = project.getCommitEditTask(appId, extension, api)

            // -------------------- START: RELEASE ONLY VARIANT SPECIFIC TASKS --------------------

            val bootstrapTask = project.newTask<Bootstrap>(
                    "bootstrap${taskVariantName}Listing",
                    """
                    |Downloads the Play Store listing metadata for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#quickstart
                    """.trimMargin(),
                    arrayOf(extension, bootstrapOptionsHolder),
            ) {
                bindApi(api)
                srcDir.set(project.file("src/${variant.flavorNameOrDefault}/$PLAY_PATH"))
            }
            bootstrapAllTask { dependsOn(bootstrapTask) }

            val resourceDir = project.newTask<GenerateResources>(
                    "generate${taskVariantName}PlayResources"
            ) {
                resDir.set(project.layout.buildDirectory.dir(variant.playPath))

                mustRunAfter(bootstrapTask)
            }.also { task ->
                // TODO(asaveau): remove once there's an API for sourceSets in the new model
                android.applicationVariants
                        .matching { it.name == variant.name }
                        .whenObjectAdded {
                            val dirs = sourceSets.map {
                                project.layout.projectDirectory.dir("src/${it.name}/$PLAY_PATH")
                            }
                            task {
                                resSrcDirs.set(dirs)
                                resSrcTree.setFrom(dirs.map {
                                    project.fileTree(it).apply { exclude("**/.*") }
                                })
                            }
                        }
            }.flatMap {
                it.resDir
            }

            val publishListingTask = project.newTask<PublishListings>(
                    "publish${taskVariantName}Listing",
                    """
                    |Uploads all Play Store metadata for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#publishing-listings
                    """.trimMargin(),
                    arrayOf(extension, executionDir),
            ) {
                bindApi(api)
                resDir.set(resourceDir)

                finalizedBy(commitEditTask)
            }
            publishListingAllTask { dependsOn(publishListingTask) }

            val publishProductsTask = project.newTask<PublishProducts>(
                    "publish${taskVariantName}Products",
                    """
                    |Uploads all Play Store in-app products for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-products
                    """.trimMargin(),
                    arrayOf(extension),
            ) {
                bindApi(api)
                productsDir.setFrom(resourceDir.map {
                    it.dir(PRODUCTS_PATH).asFileTree.matching { include("*.json") }
                })
            }
            publishProductsAllTask { dependsOn(publishProductsTask) }

            val publishSubscriptionsTask = project.newTask<PublishSubscriptions>(
                    "publish${taskVariantName}Subscriptions",
                    """
                    |Uploads all Play Store in-app subscriptions for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-subscriptions
                    """.trimMargin(),
                    arrayOf(extension),
            ) {
                bindApi(api)
                subscriptionsDir.setFrom(resourceDir.map {
                    it.dir(SUBSCRIPTIONS_PATH).asFileTree.matching { include("*.json") }
                })
            }
            publishSubscriptionsAllTask { dependsOn(publishSubscriptionsTask) }

            val isAuto = extension.resolutionStrategy.get() == ResolutionStrategy.AUTO
                    || extension.resolutionStrategy.get() == ResolutionStrategy.AUTO_OFFSET
            val staticVersionCodes = if (isAuto) {
                variant.outputs.map { it.versionCode.get() }
            } else {
                emptyList()
            }
            val processArtifactVersionCodes = project.newTask<ProcessArtifactVersionCodes>(
                    "process${taskVariantName}VersionCodes",
                    constructorArgs = arrayOf(
                            extension,
                            extension.resolutionStrategy.get() == ResolutionStrategy.AUTO_OFFSET,
                    ),
            ) {
                bindApi(api)
                versionCodes.set(staticVersionCodes)
                playVersionCodes.set(project.layout.buildDirectory.file(
                        "$INTERMEDIATES_OUTPUT_PATH/${variant.name}/available-version-codes.txt"))
            }
            if (isAuto) {
                for ((i, output) in variant.outputs.withIndex()) {
                    output.versionCode.set(processArtifactVersionCodes.map {
                        it.playVersionCodes.get().asFile.readLines()[i].toInt()
                    })
                }
            }


            fun PublishArtifactTaskBase.configureInputs() {
                bindApi(api)
                releaseNotesDir.set(resourceDir.map {
                    it.dir(RELEASE_NOTES_PATH).also { it.asFile.safeMkdirs() }
                })
                consoleNamesDir.set(resourceDir.map {
                    it.dir(RELEASE_NAMES_PATH).also { it.asFile.safeMkdirs() }
                })
            }

            val publishApkTask = project.newTask<PublishApk>(
                    "publish${taskVariantName}Apk",
                    """
                    |Uploads APK for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#publishing-apks
                    """.trimMargin(),
                    arrayOf(extension, executionDir),
            ) {
                configureInputs()
                apks.from(findApkFiles())
                mappingFiles.from(extension.artifactDir.map { customDir ->
                    project.objects.fileCollection().from(customDir.asFileTree.matching {
                        include("mapping.txt", "*.mapping.txt")
                    })
                }.orElse(project.provider {
                    variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE).takeUnless {
                        extension.artifactDir.isPresent
                    }
                }.map {
                    val files = project.objects.fileCollection()
                    if (it.isPresent) {
                        files.from(it)
                    }
                    files
                }))

                dependsOn(getArtifactDependenciesHack(SingleArtifact.APK))
                finalizedBy(commitEditTask)
                configure3pDeps(extension, taskVariantName)

                nativeDebugSymbols.set(project.layout.buildDirectory.file(
                        "outputs/native-debug-symbols/${variant.name}/native-debug-symbols.zip"
                ).map {
                    it.takeIf { it.asFile.exists() }.sneakyNull()
                })
                dependsOn("merge${taskVariantName}NativeDebugMetadata")
            }
            publishApkAllTask { dependsOn(publishApkTask) }

            val publishBundleTask = project.newTask<PublishBundle>(
                    "publish${taskVariantName}Bundle",
                    """
                    |Uploads App Bundle for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#publishing-an-app-bundle
                    """.trimMargin(),
                    arrayOf(extension, executionDir),
            ) {
                configureInputs()
                bundles.from(findBundleFiles())

                dependsOn(getArtifactDependenciesHack(SingleArtifact.BUNDLE))
                finalizedBy(commitEditTask)
                configure3pDeps(extension, taskVariantName)
            }
            publishBundleAllTask { dependsOn(publishBundleTask) }

            val promoteReleaseTask = project.newTask<PromoteRelease>(
                    "promote${taskVariantName}Artifact",
                    """
                    |Promotes a release for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#promoting-artifacts
                    """.trimMargin(),
                    arrayOf(extension, executionDir),
            ) {
                configureInputs()

                finalizedBy(commitEditTask)
                mustRunAfter(publishApkTask)
                mustRunAfter(publishBundleTask)
            }
            promoteReleaseAllTask { dependsOn(promoteReleaseTask) }

            val publishTask = project.newTask<GlobalPublishableArtifactLifecycleTask>(
                    "publish${taskVariantName}Apps",
                    """
                    |Uploads APK or App Bundle and all Play Store metadata for variant $taskVariantName.
                    |   See https://github.com/Triple-T/gradle-play-publisher#managing-artifacts
                    """.trimMargin(),
                    arrayOf(extension, executionDir),
            ) {
                dependsOn(if (extension.defaultToAppBundles.get()) {
                    publishBundleTask
                } else {
                    publishApkTask
                })
                dependsOn(publishListingTask)
                dependsOn(publishProductsTask)
                dependsOn(publishSubscriptionsTask)
            }
            publishAllTask { dependsOn(publishTask) }

            // --------------------- END: RELEASE ONLY VARIANT SPECIFIC TASKS ---------------------

            val availableGlobals = setOfNotNull(
                    variant.flavorName,
                    variant.buildType,
                    *variant.productFlavors.map { (_, flavor) -> flavor }.toTypedArray()
            ).let {
                it - variant.name
            }

            // ----------------------------- START: SEMI-GLOBAL TASKS -----------------------------

            for (global in availableGlobals) {
                val taskQualifier = global.capitalize()

                // We want to disallow using the leaf extension past this point
                @Suppress("NAME_SHADOWING")
                val extension = extensionStore[global] ?: run e@{
                    val ordering = variant.generateExtensionOverrideOrdering()

                    var i = ordering.indexOf(global)
                    while (i < ordering.size) {
                        val nextExtension = extensionStore[ordering[++i]]
                        if (nextExtension != null) {
                            return@e nextExtension
                        }
                    }
                    error("Base extension not found.")
                }

                project.newTask<BootstrapLifecycleTask>(
                        "bootstrap${taskQualifier}Listing",
                        """
                        |Downloads the Play Store listing metadata for all $taskQualifier variants.
                        |   See https://github.com/Triple-T/gradle-play-publisher#quickstart
                        """.trimMargin(),
                        arrayOf(bootstrapOptionsHolder),
                        allowExisting = true,
                ).configure { dependsOn(bootstrapTask) }
                project.newTask<GlobalPublishableArtifactLifecycleTask>(
                        "publish${taskQualifier}Apps",
                        """
                        |Uploads APK or App Bundle and all Play Store metadata for all $taskQualifier variants.
                        |   See https://github.com/Triple-T/gradle-play-publisher#managing-artifacts
                        """.trimMargin(),
                        arrayOf(extension, executionDir),
                        allowExisting = true,
                ).configure { dependsOn(publishTask) }
                project.newTask<PublishableTrackLifecycleTask>(
                        "publish${taskQualifier}Apk",
                        """
                        |Uploads APK for all $taskQualifier variants.
                        |   See https://github.com/Triple-T/gradle-play-publisher#publishing-apks
                        """.trimMargin(),
                        arrayOf(extension, executionDir),
                        allowExisting = true,
                ).configure { dependsOn(publishApkTask) }
                project.newTask<PublishableTrackLifecycleTask>(
                        "publish${taskQualifier}Bundle",
                        """
                        |Uploads App Bundle for all $taskQualifier variants.
                        |   See https://github.com/Triple-T/gradle-play-publisher#publishing-an-app-bundle
                        """.trimMargin(),
                        arrayOf(extension, executionDir),
                        allowExisting = true,
                ).configure { dependsOn(publishBundleTask) }
                project.newTask<GlobalUploadableArtifactLifecycleTask>(
                        "upload${taskQualifier}PrivateApk",
                        """
                        |Uploads Internal Sharing APK for all $taskQualifier variants.
                        |   See https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact
                        """.trimMargin(),
                        arrayOf(extension, executionDir),
                        allowExisting = true,
                ).configure { dependsOn(publishInternalSharingApkTask) }
                project.newTask<GlobalUploadableArtifactLifecycleTask>(
                        "upload${taskQualifier}PrivateBundle",
                        """
                        |Uploads Internal Sharing App Bundle for all $taskQualifier variants.
                        |   See https://github.com/Triple-T/gradle-play-publisher#uploading-an-internal-sharing-artifact
                        """.trimMargin(),
                        arrayOf(extension, executionDir),
                        allowExisting = true,
                ).configure { dependsOn(publishInternalSharingBundleTask) }
                project.newTask<UpdatableTrackLifecycleTask>(
                        "promote${taskQualifier}Artifact",
                        """
                        |Promotes a release for all $taskQualifier variants.
                        |   See https://github.com/Triple-T/gradle-play-publisher#promoting-artifacts
                        """.trimMargin(),
                        arrayOf(extension, executionDir),
                        allowExisting = true,
                ).configure { dependsOn(promoteReleaseTask) }
                project.newTask<WriteTrackLifecycleTask>(
                        "publish${taskQualifier}Listing",
                        """
                        |Uploads all Play Store metadata for all $taskQualifier variants.
                        |   See https://github.com/Triple-T/gradle-play-publisher#publishing-listings
                        """.trimMargin(),
                        arrayOf(extension, executionDir),
                        allowExisting = true,
                ).configure { dependsOn(publishListingTask) }
                project.newTask<Task>(
                        "publish${taskQualifier}Products",
                        """
                        |Uploads all Play Store in-app products for all $taskQualifier variants.
                        |   See https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-products
                        """.trimMargin(),
                        allowExisting = true,
                ).configure { dependsOn(publishProductsTask) }
                project.newTask<Task>(
                        "publish${taskQualifier}Subscriptions",
                        """
                        |Uploads all Play Store in-app subscriptions for all $taskQualifier variants.
                        |   See https://github.com/Triple-T/gradle-play-publisher#publishing-in-app-subscriptions
                        """.trimMargin(),
                        allowExisting = true,
                ).configure { dependsOn(publishSubscriptionsTask) }
            }

            // ----------------------------- END: SEMI-GLOBAL TASKS -----------------------------
        }

        project.afterEvaluate {
            val allPossiblePlayConfigNames: Set<String> by lazy {
                android.applicationVariants.flatMapTo(mutableSetOf()) { variant ->
                    listOf(variant.name, variant.buildType.name) +
                            variant.productFlavors.flatMap { listOfNotNull(it.name, it.dimension) }
                }
            }

            for (configName in extensionContainer.names) {
                if (configName !in allPossiblePlayConfigNames) {
                    project.logger.warn(
                            "Warning: Gradle Play Publisher playConfigs object '$configName' " +
                                    "does not match a variant, flavor, dimension, or build type.")
                }
            }
        }
    }

    private fun Task.configure3pDeps(extension: PlayPublisherExtension, variantName: String) {
        fun maybeAddDependency(task: String) {
            if (task in project.tasks.names) {
                dependsOn(extension.artifactDir.map {
                    listOf<String>()
                }.orElse(listOf(task)))
            }
        }

        maybeAddDependency("uploadCrashlyticsMappingFile$variantName")

        val bugsnagName = buildString {
            var seenUpper = false
            for (c in variantName) {
                if (c.isUpperCase() && seenUpper) {
                    append('-')
                    append(c.toLowerCase())
                } else {
                    append(c)
                }
                seenUpper = c.isUpperCase() || seenUpper
            }
        }
        maybeAddDependency("uploadBugsnag${bugsnagName}Mapping")

        maybeAddDependency("uploadSentryProguardMappings$variantName")
    }

    // TODO(asaveau): remove after https://github.com/gradle/gradle/issues/12388
    @Suppress("UNCHECKED_CAST")
    private fun <T> T?.sneakyNull() = this as T
}
