package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.ArtifactExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.copy
import com.github.triplet.gradle.play.tasks.internal.findApkFiles
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
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
        get() = findApkFiles(false)

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
            val response = publisher2.uploadInternalSharingApk(apkFile)

            println("Upload successful: ${response.downloadUrl}")
            parameters.outputDir.get().file("${System.currentTimeMillis()}.json").asFile
                    .writeText(response.json)
        }

        interface Params : PlayPublishingParams {
            val apkFile: RegularFileProperty
            val outputDir: DirectoryProperty
        }
    }
}
