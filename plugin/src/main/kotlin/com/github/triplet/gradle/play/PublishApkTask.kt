package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.PlayPublishPackageBase
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.TrackType.INTERNAL
import com.github.triplet.gradle.play.internal.superiors
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
        //TODO: If we take in a folder here as an option, we can fix #233, #227
        val publishedApks = publishApks(editId).filterNotNull()
        if (publishedApks.isNotEmpty()) {
            updateTracks(editId, inputFolder, publishedApks.map { it.versionCode.toLong() })
        }
    }

    private fun AndroidPublisher.Edits.publishApks(editId: String) = variant.outputs
            .filterIsInstance<ApkVariantOutput>()
            .map { publishApk(editId, FileContent(MIME_TYPE_APK, it.outputFile)) }

    private fun AndroidPublisher.Edits.publishApk(editId: String, apkFile: FileContent): Apk? {
        val apk = try {
            apks().upload(variant.applicationId, editId, apkFile).execute()
        } catch (e: GoogleJsonResponseException) {
            val isConflict = e.details.errors.all {
                it.reason == "apkUpgradeVersionConflict" || it.reason == "apkNoUpgradePath"
            }
            if (isConflict) {
                when (extension._resolutionStrategy) {
                    ResolutionStrategy.AUTO -> throw IllegalStateException(
                            "Concurrent uploads for variant ${variant.name}. Make sure to " +
                                    "synchronously upload your APKs such that they don't conflict.",
                            e
                    )
                    ResolutionStrategy.FAIL -> throw IllegalStateException(
                            "Version code ${variant.versionCode} is too low for variant ${variant.name}.",
                            e
                    )
                    ResolutionStrategy.IGNORE -> logger.warn(
                            "Ignoring APK ($apkFile) for version code ${variant.versionCode}")
                }
                return null
            } else {
                throw e
            }
        }

        if (extension.untrackOld && extension._track != INTERNAL) {
            extension._track.superiors.map { it.publishedName }.forEach { channel ->
                try {
                    val track = tracks().get(
                            variant.applicationId,
                            editId,
                            channel
                    ).execute().apply {
                        releases.forEach {
                            it.versionCodes =
                                    it.versionCodes.filter { it > apk.versionCode.toLong() }
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
