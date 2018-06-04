package com.github.triplet.gradle.play.internal

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import java.io.File

abstract class PlayPublishPackageBase : PlayPublishTaskBase() {
    protected fun AndroidPublisher.Edits.updateTracks(
            editId: String,
            inputFolder: File,
            versions: List<Long>
    ) {
        val track = tracks()
                .list(variant.applicationId, editId)
                .execute().tracks
                ?.firstOrNull { it.track == extension.track } ?: Track()

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
            status = extension.releaseStatus
            userFraction = if (extension._releaseStatus == ReleaseStatus.INPROGRESS) {
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
