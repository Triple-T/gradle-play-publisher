package com.github.triplet.gradle.play.internal

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

abstract class PlayPublishPackageBase : PlayPublishTaskBase() {
    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    lateinit var resDir: File

    protected fun AndroidPublisher.Edits.updateTracks(editId: String, versions: List<Long>) {
        val track = tracks()
                .list(variant.applicationId, editId)
                .execute().tracks
                ?.firstOrNull { it.track == extension.track } ?: Track()

        val releaseTexts = resDir.orNull()?.listFiles(LocaleFileFilter)?.mapNotNull { locale ->
            val fileName = ListingDetail.WHATS_NEW.fileName
            val file = File(locale, "$fileName-${extension.track}").orNull()
                    ?: File(locale, fileName).orNull()
                    ?: return@mapNotNull null

            val recentChanges = file.readProcessed(
                    ListingDetail.WHATS_NEW.maxLength,
                    extension.errorOnSizeLimit
            )
            LocalizedText().setLanguage(locale.name).setText(recentChanges)
        }
        val trackRelease = TrackRelease().apply {
            releaseNotes = releaseTexts
            status = extension.releaseStatus
            userFraction = if (extension._releaseStatus == ReleaseStatus.IN_PROGRESS) {
                extension.userFraction
            } else {
                null
            }
            versionCodes = versions
        }

        track.releases = listOf(trackRelease)

        tracks()
                .update(variant.applicationId, editId, extension.track, track)
                .execute()
    }
}
