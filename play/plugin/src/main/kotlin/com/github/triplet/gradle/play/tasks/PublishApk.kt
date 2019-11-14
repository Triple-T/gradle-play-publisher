package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.releaseStatusOrDefault
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.github.triplet.gradle.play.internal.trackOrDefault
import com.github.triplet.gradle.play.internal.userFractionOrDefault
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.findApkFiles
import com.github.triplet.gradle.play.tasks.internal.workers.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.copy
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

internal abstract class PublishApk @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishArtifactTaskBase(extension, variant), PublishableTrackExtensionOptions {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val apks
        get() = findApkFiles(true)

    // This directory isn't used, but it's needed for up-to-date checks to work
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:Optional
    @get:OutputDirectory
    protected val outputDir = null

    @TaskAction
    fun publishApks() {
        val apks = apks.orEmpty().mapNotNull(File::orNull).ifEmpty { return }

        project.delete(temporaryDir) // Make sure previous executions get cleared out
        project.serviceOf<WorkerExecutor>().noIsolation().submit(Processor::class) {
            paramsForBase(this)

            apkFiles.set(apks)
            uploadResults.set(temporaryDir)
        }
    }

    abstract class Processor @Inject constructor(
            private val executor: WorkerExecutor
    ) : ArtifactWorkerBase<Processor.Params>() {
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
            edits2.publishApk(
                    versions,
                    parameters.skippedMarker.get().asFile.exists(),
                    config.trackOrDefault,
                    config.releaseStatusOrDefault,
                    findReleaseName(config.trackOrDefault),
                    findReleaseNotes(config.trackOrDefault),
                    config.userFractionOrDefault,
                    config.retain.artifacts
            )
        }

        interface Params : ArtifactPublishingParams {
            val apkFiles: ListProperty<File>
            val uploadResults: DirectoryProperty
        }
    }

    abstract class ApkUploader : ArtifactWorkerBase<ApkUploader.Params>() {
        init {
            commit = false
        }

        override fun upload() {
            val apkFile = parameters.apkFile.get().asFile
            val versionCode = edits2.uploadApk(
                    apkFile,
                    parameters.mappingFile.orNull?.asFile,
                    config.resolutionStrategyOrDefault,
                    findBestVersionCode(apkFile),
                    parameters.variantName.get(),
                    config.retain.mainObb,
                    config.retain.patchObb
            ) ?: return

            parameters.uploadResults.get().file(versionCode.toString()).asFile.safeCreateNewFile()
        }

        interface Params : ArtifactPublishingParams {
            val apkFile: RegularFileProperty
            val uploadResults: DirectoryProperty
        }
    }
}
