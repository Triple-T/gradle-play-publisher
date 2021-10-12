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
import org.gradle.api.tasks.InputFile
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
internal abstract class PublishApk @Inject constructor(
        extension: PlayPublisherExtension,
        executionDir: Directory,
        private val fileOps: FileSystemOperations,
        private val executor: WorkerExecutor,
) : PublishArtifactTaskBase(extension),
        PublishableTrackExtensionOptions by CliOptionsImpl(extension, executionDir) {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    internal abstract val apks: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    internal abstract val mappingFiles: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputFile
    internal abstract val nativeDebugSymbols: RegularFileProperty

    // This directory isn't used, but it's needed for up-to-date checks to work
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:Optional
    @get:OutputDirectory
    protected val outputDir = null

    @TaskAction
    fun publishApks() {
        fileOps.delete { delete(temporaryDir) } // Make sure previous executions get cleared out
        executor.noIsolation().submit(Processor::class) {
            paramsForBase(this)

            apkFiles.set(apks)
            deobfuscationFiles.from(mappingFiles)
            debugSymbolsFile.set(nativeDebugSymbols)
            uploadResults.set(temporaryDir)
        }
    }

    abstract class Processor @Inject constructor(
            private val executor: WorkerExecutor,
    ) : PublishArtifactWorkerBase<Processor.Params>() {
        override fun upload() {
            val deobFiles = parameters.deobfuscationFiles.files.associateBy {
                it.name.removeSuffix("mapping.txt").removeSuffix(".")
            }

            for (apk in parameters.apkFiles.get()) {
                executor.noIsolation().submit(ApkUploader::class) {
                    parameters.copy(this)

                    apkFile.set(apk)
                    mappingFile.set(deobFiles.getOrDefault(
                            apk.nameWithoutExtension,
                            // The empty name is the default file called "mapping.txt"
                            deobFiles[""]
                    ))
                    debugSymbolsFile.set(parameters.debugSymbolsFile)
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
            val apkFiles: ListProperty<File>
            val deobfuscationFiles: ConfigurableFileCollection
            val debugSymbolsFile: RegularFileProperty
            val uploadResults: DirectoryProperty
        }
    }

    abstract class ApkUploader : PublishArtifactWorkerBase<ApkUploader.Params>() {
        init {
            commit = false
        }

        override fun upload() {
            val apkFile = parameters.apkFile.get().asFile
            val versionCode = apiService.edits.uploadApk(
                    apkFile,
                    parameters.mappingFile.orNull?.asFile,
                    parameters.debugSymbolsFile.orNull?.asFile,
                    config.resolutionStrategy,
                    config.retainMainObb,
                    config.retainPatchObb
            ) ?: return

            parameters.uploadResults.get().file(versionCode.toString()).asFile.safeCreateNewFile()
        }

        interface Params : ArtifactPublishingParams {
            val apkFile: RegularFileProperty
            val mappingFile: RegularFileProperty
            val debugSymbolsFile: RegularFileProperty
            val uploadResults: DirectoryProperty
        }
    }
}
