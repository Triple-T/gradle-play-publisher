package com.github.triplet.gradle.play.tasks

import com.android.build.VariantOutput.OutputType
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.trackUploadProgress
import com.github.triplet.gradle.play.tasks.internal.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PlayPublishArtifactBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ExpansionFile
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

open class PublishApk @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PlayPublishArtifactBase(extension, variant), PublishableTrackExtensionOptions {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val inputApks: List<File>?
        get() {
            val customDir = extension._artifactDir

            return if (customDir == null) {
                variant.outputs.filterIsInstance<ApkVariantOutput>().filter {
                    OutputType.valueOf(it.outputType) == OutputType.MAIN || it.filters.isNotEmpty()
                }.map { it.outputFile }
            } else {
                customDir.listFiles().orEmpty().filter { it.extension == "apk" }.also {
                    if (it.isEmpty()) println("Warning: no APKs found in '$customDir' yet.")
                }
            }.ifEmpty { null }
        }
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory // This directory isn't used, but it's needed for up-to-date checks to work
    protected val outputDir by lazy { File(project.buildDir, "${variant.playPath}/apks") }

    @TaskAction
    fun publishApks() {
        val apks = inputApks.orEmpty().mapNotNull(File::orNull).ifEmpty { return }
        project.serviceOf<WorkerExecutor>().submit(ApkUploader::class) {
            paramsForBase(this, ApkUploader.Params(apks))
        }
    }

    private class ApkUploader @Inject constructor(
            private val p: Params,
            artifact: ArtifactPublishingData,
            val play: PlayPublishingData
    ) : ArtifactWorkerBase(artifact, play) {
        override fun upload() {
            updateTracks(editId, p.apkFiles.mapNotNull {
                var code = uploadApk(editId, FileContent(MIME_TYPE_APK, it))?.versionCode?.toLong()

                // Attach Obb file from other APK (using its code)
                var expansionFile = ExpansionFile()
                expansionFile.referencesVersion = play.extension.attachObb
                edits.expansionfiles().update(appId, editId, code!!.toInt(), "main", expansionFile).execute()

                code
            }.ifEmpty { return })
        }

        private fun uploadApk(editId: String, content: FileContent): Apk? {
            val apk = try {
                edits.apks().upload(appId, editId, content).trackUploadProgress("APK").execute()
            } catch (e: GoogleJsonResponseException) {
                return handleUploadFailures(e, content.file)
            }

            handleArtifactDetails(editId, apk.versionCode)

            return apk
        }

        data class Params(val apkFiles: List<File>) : Serializable

        private companion object {
            const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        }
    }
}
