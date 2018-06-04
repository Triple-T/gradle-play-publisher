package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.ApkFileFilter
import com.github.triplet.gradle.play.internal.PlayPublishPackageBase
import com.github.triplet.gradle.play.internal.TrackType.INTERNAL
import com.github.triplet.gradle.play.internal.superiors
import com.github.triplet.gradle.play.internal.ApkFileFilter
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import org.gradle.api.tasks.TaskAction
import java.io.File

open class PublishApkTask : PlayPublishPackageBase() {
    lateinit var inputFolder: File

    @TaskAction
    fun publishApks() = write { editId: String ->
        val files = extension.buildInputFolder?.listFiles(ApkFileFilter)?.toList()
                ?: variant.outputs.filter { it is ApkVariantOutput }.map { it.outputFile }
        val publishedApks = publishApks(editId, files)
        updateTracks(editId, inputFolder, publishedApks.map { it.versionCode.toLong() })
    }

    private fun AndroidPublisher.Edits.publishApks(editId: String, apks: List<File>) =
            apks.map { publishApk(editId, FileContent(MIME_TYPE_APK, it)) }

    private fun AndroidPublisher.Edits.publishApk(editId: String, apkFile: FileContent): Apk {
        val apk = apks().upload(variant.applicationId, editId, apkFile).execute()

        if (extension.untrackOld && extension._track != INTERNAL) {
            extension._track.superiors.map { it.publishedName }.forEach { channel ->
                try {
                    val track = tracks().get(
                            variant.applicationId,
                            editId,
                            channel
                    ).execute().apply {
                        releases.forEach {
                            it.versionCodes = it.versionCodes.filter { it > apk.versionCode.toLong() }
                        }
                    }
                    tracks().update(variant.applicationId, editId, channel, track).execute()
                } catch (e: GoogleJsonResponseException) {
                    // Just skip if there is no version in track
                    if (e.details.code != 404) throw e
                }
            }
        }

        // Upload Proguard mapping.txt if available
        if (variant.mappingFile?.exists() == true) {
            val content = FileContent(MIME_TYPE_STREAM, variant.mappingFile)
            deobfuscationfiles()
                    .upload(variant.applicationId, editId, apk.versionCode, "proguard", content)
                    .execute()
        }

        return apk
    }

    private companion object {
        const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        const val MIME_TYPE_STREAM = "application/octet-stream"
    }
}
