package com.github.triplet.gradle.play.tasks

import com.android.build.VariantOutput.OutputType
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.tasks.internal.ArtifactExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.http.FileContent
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

abstract class PublishInternalSharingApk @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PlayPublishTaskBase(extension, variant), ArtifactExtensionOptions {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    protected val apk: File?
        get() {
            val customDir = extension._artifactDir

            return if (customDir == null) {
                variant.outputs.filterIsInstance<ApkVariantOutput>().singleOrNull {
                    OutputType.valueOf(it.outputType) == OutputType.MAIN || it.filters.isEmpty()
                }?.outputFile
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
        val apkFile = apk?.orNull() ?: return
        project.serviceOf<WorkerExecutor>().submit(ApkUploader::class) {
            paramsForBase(this, ApkUploader.Params(apkFile, outputDir))
        }
    }

    private class ApkUploader @Inject constructor(
            private val p: Params,
            play: PlayPublishingData
    ) : PlayWorkerBase(play) {
        override fun run() {
            val apk = publisher.internalappsharingartifacts()
                    .uploadapk(appId, FileContent(MIME_TYPE_APK, p.apkFile))
                    .trackUploadProgress("APK", p.apkFile)
                    .execute()

            File(p.outputDir, "${System.currentTimeMillis()}.json").writeText(apk.toPrettyString())
            println("Upload successful: ${apk.downloadUrl}")
        }

        data class Params(val apkFile: File, val outputDir: File) : Serializable

        private companion object {
            const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        }
    }
}
