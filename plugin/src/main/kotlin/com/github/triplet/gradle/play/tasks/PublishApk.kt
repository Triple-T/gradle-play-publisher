package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.tasks.internal.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.TransientTrackOptions
import com.github.triplet.gradle.play.tasks.internal.copy
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.ExpansionFile
import org.gradle.api.provider.Property
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

abstract class PublishApk @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant,
        optionsHolder: TransientTrackOptions.Holder
) : PublishArtifactTaskBase(extension, variant, optionsHolder), PublishableTrackExtensionOptions {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val inputApks: List<File>?
        get() {
            val customDir = extension.config.artifactDir

            return if (customDir == null) {
                variant.outputs.filterIsInstance<ApkVariantOutput>().map { it.outputFile }
            } else if (customDir.isFile && customDir.extension == "apk") {
                listOf(customDir)
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
        project.serviceOf<WorkerExecutor>().noIsolation().submit(ApksUploader::class) {
            paramsForBase(this)

            apkFiles.set(apks)
            uploadResults.set(temporaryDir)
        }
    }

    internal abstract class ApksUploader @Inject constructor(
            private val executor: WorkerExecutor
    ) : ArtifactWorkerBase<ApksUploader.Params>() {
        override fun upload() {
            for (apk in parameters.apkFiles.get()) {
                executor.noIsolation().submit(Uploader::class) {
                    parameters.copy(this)

                    this.apk.set(apk)
                    this.uploadResults.set(parameters.uploadResults.get())
                }
            }
            executor.await()

            val versions = parameters.uploadResults.get().listFiles().orEmpty().map {
                it.name.toLong()
            }
            updateTracks(versions)
        }

        interface Params : ArtifactPublishingParams {
            val apkFiles: Property<List<File>>
            val uploadResults: Property<File>
        }
    }

    internal abstract class Uploader : ArtifactWorkerBase<Uploader.Params>() {
        init {
            commit = false
        }

        override fun upload() {
            val apk = try {
                edits.apks()
                        .upload(appId, editId, FileContent(MIME_TYPE_APK, parameters.apk.get()))
                        .trackUploadProgress("APK", parameters.apk.get())
                        .execute()
            } catch (e: GoogleJsonResponseException) {
                handleUploadFailures(e, parameters.apk.get())
                return
            }

            config.retain.mainObb?.attachObb(apk.versionCode, "main")
            config.retain.patchObb?.attachObb(apk.versionCode, "patch")

            uploadMappingFile(apk.versionCode)
            File(parameters.uploadResults.get(), apk.versionCode.toString()).createNewFile()
        }

        private fun Int.attachObb(versionCode: Int, type: String) {
            println("Attaching $type OBB ($this) to APK $versionCode")
            val obb = ExpansionFile().also { it.referencesVersion = this }
            edits.expansionfiles()
                    .update(appId, editId, versionCode, type, obb)
                    .execute()
        }

        interface Params : ArtifactPublishingParams {
            val apk: Property<File>
            val uploadResults: Property<File>
        }

        private companion object {
            const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        }
    }
}
