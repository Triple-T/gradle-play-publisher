package com.github.triplet.gradle.play.tasks

import com.android.build.VariantOutput.OutputType
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.ArtifactExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.copy
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.http.FileContent
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

internal abstract class PublishInternalSharingApk @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishTaskBase(extension, variant), ArtifactExtensionOptions {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val apks: List<File>?
        get() {
            val customDir = extension.config.artifactDir

            return if (customDir == null) {
                variant.outputs.filterIsInstance<ApkVariantOutput>().filter {
                    OutputType.valueOf(it.outputType) == OutputType.MAIN || it.filters.isEmpty()
                }.map { it.outputFile }
            } else if (customDir.isFile && customDir.extension == "apk") {
                listOf(customDir)
            } else {
                val apks = customDir.listFiles().orEmpty().filter { it.extension == "apk" }
                if (apks.isEmpty()) {
                    logger.warn("Warning: '$customDir' does not yet contain any APKs.")
                }
                apks
            }.ifEmpty { null }
        }

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun publishApk() {
        val apks = apks.orEmpty().mapNotNull(File::orNull).ifEmpty { return }

        project.serviceOf<WorkerExecutor>().noIsolation().submit(Processor::class) {
            paramsForBase(this)

            apkFiles.set(apks)
            outputDir.set(outputDirectory)
        }
    }

    abstract class Processor @Inject constructor(
            private val executor: WorkerExecutor
    ) : PlayWorkerBase<Processor.Params>() {
        override fun execute() {
            for (apk in parameters.apkFiles.get()) {
                executor.noIsolation().submit(ApkUploader::class) {
                    parameters.copy(this)

                    apkFile.set(apk)
                    outputDir.set(parameters.outputDir)
                }
            }
        }

        interface Params : PlayPublishingParams {
            val apkFiles: ListProperty<File>
            val outputDir: DirectoryProperty
        }
    }

    abstract class ApkUploader : PlayWorkerBase<ApkUploader.Params>() {
        override fun execute() {
            val apkFile = parameters.apkFile.get().asFile
            val apk = publisher.internalappsharingartifacts()
                    .uploadapk(appId, FileContent(MIME_TYPE_APK, apkFile))
                    .trackUploadProgress("APK", apkFile)
                    .execute()

            parameters.outputDir.get().file("${System.currentTimeMillis()}.json").asFile
                    .writeText(apk.toPrettyString())
            println("Upload successful: ${apk.downloadUrl}")
        }

        interface Params : PlayPublishingParams {
            val apkFile: RegularFileProperty
            val outputDir: DirectoryProperty
        }

        private companion object {
            const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        }
    }
}
