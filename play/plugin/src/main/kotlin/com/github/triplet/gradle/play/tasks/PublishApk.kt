package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.ArtifactWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishableTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.copy
import com.github.triplet.gradle.play.tasks.internal.findApkFiles
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.ExpansionFile
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
            }
            updateTracks(versions)
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
            val apk = try {
                edits.apks()
                        .upload(appId, editId, FileContent(MIME_TYPE_APK, apkFile))
                        .trackUploadProgress("APK", apkFile)
                        .execute()
            } catch (e: GoogleJsonResponseException) {
                handleUploadFailures(e, apkFile)
                return
            }

            config.retain.mainObb?.attachObb(apk.versionCode, "main")
            config.retain.patchObb?.attachObb(apk.versionCode, "patch")

            uploadMappingFile(apk.versionCode)
            parameters.uploadResults.get().file(apk.versionCode.toString()).asFile.createNewFile()
        }

        private fun Int.attachObb(versionCode: Int, type: String) {
            println("Attaching $type OBB ($this) to APK $versionCode")
            val obb = ExpansionFile().also { it.referencesVersion = this }
            edits.expansionfiles()
                    .update(appId, editId, versionCode, type, obb)
                    .execute()
        }

        interface Params : ArtifactPublishingParams {
            val apkFile: RegularFileProperty
            val uploadResults: DirectoryProperty
        }

        private companion object {
            const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        }
    }
}
