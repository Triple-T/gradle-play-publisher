package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.tasks.internal.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PlayPublishArtifactBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.TransientTrackOptions
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

abstract class PublishApk @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        variant: ApplicationVariant,
        optionsHolder: TransientTrackOptions.Holder
) : PlayPublishArtifactBase(extension, variant, optionsHolder), PublishableTrackExtensionOptions {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val inputApks: List<File>?
        get() {
            val customDir = extension._artifactDir

            return if (customDir == null) {
                variant.outputs.filterIsInstance<ApkVariantOutput>().map { it.outputFile }
            } else {
                customDir.listFiles().orEmpty().filter { it.extension == "apk" }.also {
                    if (it.isEmpty()) logger.warn("Warning: no APKs found in '$customDir' yet.")
                }
            }.ifEmpty { null }
        }
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:OutputDirectory // This directory isn't used, but it's needed for up-to-date checks to work
    protected val outputDir by lazy { File(project.buildDir, "${variant.playPath}/apks") }

    @TaskAction
    fun publishApks() {
        val apks = inputApks.orEmpty().mapNotNull(File::orNull).ifEmpty { return }

        project.delete(temporaryDir) // Make sure previous executions get cleared out
        project.serviceOf<WorkerExecutor>().submit(ApksUploader::class) {
            isolationMode = IsolationMode.NONE
            paramsForBase(this, ApksUploader.Params(apks, temporaryDir), getOrCreateEditId())
        }
    }

    private class ApksUploader @Inject constructor(
            private val executor: WorkerExecutor,

            private val p: Params,
            private val artifact: ArtifactPublishingData,
            private val play: PlayPublishingData
    ) : ArtifactWorkerBase(artifact, play) {
        override fun upload() {
            for (apk in p.apkFiles) {
                executor.submit(Uploader::class) {
                    params(Uploader.Params(apk, p.uploadResults), artifact, play)
                }
            }
            executor.await()

            val versions = p.uploadResults.listFiles().orEmpty().map { it.name.toLong() }
            updateTracks(editId, versions)
        }

        data class Params(val apkFiles: List<File>, val uploadResults: File) : Serializable

        private class Uploader @Inject constructor(
                private val p: Params,
                artifact: ArtifactPublishingData,
                play: PlayPublishingData
        ) : ArtifactWorkerBase(artifact, play) {
            init {
                commit = false
            }

            override fun upload() {
                val apk = try {
                    edits.apks()
                            .upload(appId, editId, FileContent(MIME_TYPE_APK, p.apk))
                            .trackUploadProgress("APK", p.apk)
                            .execute()
                } catch (e: GoogleJsonResponseException) {
                    handleUploadFailures(e, p.apk)
                    return
                }

                handleArtifactDetails(editId, apk.versionCode)
                File(p.uploadResults, apk.versionCode.toString()).createNewFile()
            }

            data class Params(val apk: File, val uploadResults: File) : Serializable

            private companion object {
                const val MIME_TYPE_APK = "application/vnd.android.package-archive"
            }
        }
    }
}
