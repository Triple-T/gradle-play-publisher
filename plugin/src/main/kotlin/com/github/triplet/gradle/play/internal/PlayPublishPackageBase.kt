package com.github.triplet.gradle.play.internal

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import java.io.File

abstract class PlayPublishPackageBase : PlayPublishTaskBase() {
    internal fun AndroidPublisher.Edits.updateTracks(
            editId: String,
            inputFolder: File,
            releaseStatus: ReleaseStatus,
            versions: List<Long>,
            trackType: String,
            userPercent: Double
    ) {
        val track = tracks()
                .list(variant.applicationId, editId)
                .execute().tracks
                ?.firstOrNull { it.track == trackType } ?: Track()

        val releaseTexts = if (inputFolder.exists()) {
            inputFolder.listFiles(LocaleFileFilter).mapNotNull { locale ->
                val fileName = ListingDetail.WHATS_NEW.fileName
                val file = run {
                    File(locale, "$fileName-${extension.track}").orNull()
                            ?: File(locale, fileName).orNull()
                } ?: return@mapNotNull null

                val recentChanges = File(file, fileName).readProcessed(
                        ListingDetail.WHATS_NEW.maxLength,
                        extension.errorOnSizeLimit
                )
                LocalizedText().setLanguage(locale.name).setText(recentChanges)
            }
        } else {
            null
        }
        val trackRelease = TrackRelease().apply {
            releaseNotes = releaseTexts
            status = releaseStatus.status
            userFraction = if (status == ReleaseStatus.IN_PROGRESS.status) userPercent else 0.0
            versionCodes = versions
        }

        track.releases = listOf(trackRelease)

        tracks()
                .update(variant.applicationId, editId, trackType, track)
                .execute()
    }
}
