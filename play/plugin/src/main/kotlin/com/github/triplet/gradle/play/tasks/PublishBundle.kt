package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.CliOptionsImpl
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.workers.PublishArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.copy
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class PublishBundle @Inject constructor(
        extension: PlayPublisherExtension,
        executionDir: Directory,
        private val fileOps: FileSystemOperations,
        private val executor: WorkerExecutor,
) : PublishArtifactTaskBase(extension),
        PublishableTrackExtensionOptions by CliOptionsImpl(extension, executionDir) {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    internal abstract val bundles: ConfigurableFileCollection

    // This directory isn't used, but it's needed for up-to-date checks to work
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:Optional
    @get:OutputDirectory
    protected val outputDir = null

    @TaskAction
    fun publishBundles() {
        fileOps.delete { delete(temporaryDir) } // Make sure previous executions get cleared out
        executor.noIsolation().submit(Processor::class) {
            paramsForBase(this)

            bundleFiles.set(bundles)
            uploadResults.set(temporaryDir)
        }
    }

    abstract class Processor @Inject constructor(
            private val executor: WorkerExecutor,
    ) : PublishArtifactWorkerBase<Processor.Params>() {
        override fun upload() {
            for (bundle in parameters.bundleFiles.get()) {
                executor.noIsolation().submit(BundleUploader::class) {
                    parameters.copy(this)

                    bundleFile.set(bundle)
                    uploadResults.set(parameters.uploadResults)
                }
            }
            executor.await()

            val versions = parameters.uploadResults.asFileTree.map {
                it.name.toLong()
            }.sorted()
            apiService.edits.publishArtifacts(
                    versions,
                    apiService.shouldSkip(),
                    config.track,
                    config.releaseStatus,
                    findReleaseName(config.track),
                    findReleaseNotes(config.track),
                    config.userFraction,
                    config.updatePriority,
                    config.retainArtifacts
            )
        }

        interface Params : ArtifactPublishingParams {
            val bundleFiles: ListProperty<File>
            val uploadResults: DirectoryProperty
        }
    }

    abstract class BundleUploader : PublishArtifactWorkerBase<BundleUploader.Params>() {
        init {
            commit = false
        }

        override fun upload() {
            val bundleFile = parameters.bundleFile.get().asFile
            val versionCode = apiService.edits.uploadBundle(
                    bundleFile,
                    config.resolutionStrategy
            ) ?: return

            parameters.uploadResults.get().file(versionCode.toString()).asFile.safeCreateNewFile()
        }

        interface Params : ArtifactPublishingParams {
            val bundleFile: RegularFileProperty
            val uploadResults: DirectoryProperty
        }
    }
}
