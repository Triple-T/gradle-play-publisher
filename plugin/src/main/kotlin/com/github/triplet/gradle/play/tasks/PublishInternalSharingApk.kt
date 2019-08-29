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
import org.gradle.api.provider.Property
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
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
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
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:OutputDirectory
    protected val outputDir by lazy {
        File(project.buildDir, "outputs/internal-sharing/apk/${variant.name}")
    }

    @TaskAction
    fun publishApk() {
        val apk = apk?.orNull() ?: return
        project.serviceOf<WorkerExecutor>().noIsolation().submit(ApkUploader::class) {
            paramsForBase(this)

            apkFile.set(apk)
            outputDir.set(this@PublishInternalSharingApk.outputDir)
        }
    }

    internal abstract class ApkUploader : PlayWorkerBase<ApkUploader.Params>() {
        override fun execute() {
            val apk = publisher.internalappsharingartifacts()
                    .uploadapk(appId, FileContent(MIME_TYPE_APK, parameters.apkFile.get()))
                    .trackUploadProgress("APK", parameters.apkFile.get())
                    .execute()

            File(parameters.outputDir.get(), "${System.currentTimeMillis()}.json")
                    .writeText(apk.toPrettyString())
            println("Upload successful: ${apk.downloadUrl}")
        }

        interface Params : PlayPublishingParams {
            val apkFile: Property<File>
            val outputDir: Property<File>
        }

        private companion object {
            const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        }
    }
}
