package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.UploadArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.UploadArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.copy
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
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
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

internal abstract class PublishApk @Inject constructor(
        extension: PlayPublisherExtension,
        appId: String
) : UploadArtifactTaskBase(extension, appId), PublishableTrackExtensionOptions {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    internal abstract val apks: ConfigurableFileCollection

    // This directory isn't used, but it's needed for up-to-date checks to work
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:Optional
    @get:OutputDirectory
    protected val outputDir = null

    @TaskAction
    fun publishApks() {
        project.delete(temporaryDir) // Make sure previous executions get cleared out
        project.serviceOf<WorkerExecutor>().noIsolation().submit(Processor::class) {
            paramsForBase(this)

            apkFiles.set(apks)
            uploadResults.set(temporaryDir)
        }
    }

    abstract class Processor @Inject constructor(
            private val executor: WorkerExecutor
    ) : UploadArtifactWorkerBase<Processor.Params>() {
        override fun upload() {
            for (apk in parameters.apkFiles.get()) {
                executor.noIsolation().submit(ApkUploader::class) {
                    parameters.copy(this)

                    apkFile.set(apk)
                    uploadResults.set(parameters.uploadResults)
                }
            }
            executor.await()

            val versions = parameters.uploadResults.asFileTree.map {
                it.name.toLong()
            }.sorted()
            edits.publishApk(
                    versions,
                    parameters.skippedMarker.get().asFile.exists(),
                    config.track,
                    config.releaseStatus,
                    findReleaseName(config.track),
                    findReleaseNotes(config.track),
                    config.userFraction,
                    config.updatePriority,
                    config.retainArtifacts
            )
        }

        interface Params : ArtifactUploadingParams {
            val apkFiles: ListProperty<File>
            val uploadResults: DirectoryProperty
        }
    }

    abstract class ApkUploader : UploadArtifactWorkerBase<ApkUploader.Params>() {
        init {
            commit = false
        }

        override fun upload() {
            val apkFile = parameters.apkFile.get().asFile
            val versionCode = edits.uploadApk(
                    apkFile,
                    parameters.mappingFile.orNull?.asFile,
                    config.resolutionStrategy,
                    config.retainMainObb,
                    config.retainPatchObb
            ) ?: return

            parameters.uploadResults.get().file(versionCode.toString()).asFile.safeCreateNewFile()
        }

        interface Params : ArtifactUploadingParams {
            val apkFile: RegularFileProperty
            val uploadResults: DirectoryProperty
        }
    }
}
