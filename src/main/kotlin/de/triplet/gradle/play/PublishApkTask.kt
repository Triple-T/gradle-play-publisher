package de.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.Track
import de.triplet.gradle.play.internal.ListingDetail
import de.triplet.gradle.play.internal.LocaleFileFilter
import de.triplet.gradle.play.internal.PlayPublishTaskBase
import de.triplet.gradle.play.internal.Track.INTERNAL
import de.triplet.gradle.play.internal.orNull
import de.triplet.gradle.play.internal.readProcessed
import de.triplet.gradle.play.internal.superiors
import org.gradle.api.tasks.TaskAction
import java.io.File

open class PublishApkTask : PlayPublishTaskBase() {
    lateinit var inputFolder: File

    @TaskAction
    fun publishApks() = write { editId: String ->
        val publishedApks = publishApks(editId)
        updateTracks(editId, publishedApks)
    }

    private fun AndroidPublisher.Edits.publishApks(editId: String) = variant.outputs
            .filter { it is ApkVariantOutput }
            .map { publishApk(editId, FileContent(MIME_TYPE_APK, it.outputFile)) }

    private fun AndroidPublisher.Edits.updateTracks(editId: String, publishedApks: List<Apk>) {
        tracks()
                .update(variant.applicationId, editId, extension.track, Track()
                        .setVersionCodes(publishedApks.map { it.versionCode })
                        .setUserFraction(extension.userFraction))
                .execute()
    }

    private fun AndroidPublisher.Edits.publishApk(editId: String, apkFile: FileContent): Apk {
        val apk = apks().upload(variant.applicationId, editId, apkFile).execute()

        fun updateWhatsNew(locale: File) {
            val fileName = ListingDetail.WHATS_NEW.fileName
            val file = run {
                var file = File(locale, "$fileName-${extension.track}").orNull()
                if (file == null) file = File(locale, fileName).orNull()
                file
            } ?: return

            val listing = ApkListing().apply {
                recentChanges = File(file, fileName).readProcessed(
                        ListingDetail.WHATS_NEW.maxLength,
                        extension.errorOnSizeLimit
                )
            }

            apklistings()
                    .update(variant.applicationId, editId, apk.versionCode, locale.name, listing)
                    .execute()
        }

        if (extension.untrackOld && extension._track != INTERNAL) {
            extension._track.superiors.map { it.publishedName }.forEach { channel ->
                try {
                    val track = tracks().get(
                            variant.applicationId,
                            editId,
                            channel
                    ).execute().apply {
                        versionCodes = versionCodes.filter { it > apk.versionCode }
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
            val content = FileContent("application/octet-stream", variant.mappingFile)
            deobfuscationfiles()
                    .upload(variant.applicationId, editId, apk.versionCode, "proguard", content)
                    .execute()
        }

        if (inputFolder.exists()) {
            // Matches valid locales e.g. en-US for Play Store
            inputFolder.listFiles(LocaleFileFilter).forEach { updateWhatsNew(it) }
        }

        return apk
    }

    private companion object {
        const val MIME_TYPE_APK = "application/vnd.android.package-archive"
    }
}
