package com.github.triplet.gradle.play.tasks

import com.android.build.VariantOutput.OutputType
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.tasks.internal.ArtifactExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.http.FileContent
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class PublishInternalSharingApk @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishTaskBase(extension, variant), ArtifactExtensionOptions {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    protected val apk: File?
        get() {
            val customDir = extension.config.artifactDir

            return if (customDir == null) {
                variant.outputs.filterIsInstance<ApkVariantOutput>().singleOrNull {
                    OutputType.valueOf(it.outputType) == OutputType.MAIN || it.filters.isEmpty()
                }?.outputFile
            } else if (customDir.isFile && customDir.extension == "apk") {
                customDir
            } else {
                customDir.listFiles().orEmpty().singleOrNull { it.extension == "apk" }.also {
                    if (it == null) logger.warn("Warning: no APKs found in '$customDir' yet.")
                }
            }
        }

    @get:OutputDirectory
    internal abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun publishApk() {
        val apk = apk?.orNull() ?: return
        project.serviceOf<WorkerExecutor>().noIsolation().submit(ApkUploader::class) {
            paramsForBase(this)

            apkFile.set(apk)
            outputDir.set(outputDirectory)
        }
    }

    internal abstract class ApkUploader : PlayWorkerBase<ApkUploader.Params>() {
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
