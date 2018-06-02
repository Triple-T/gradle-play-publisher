package de.triplet.gradle.play.internal

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import java.io.File

abstract class PlayPublishPackageBase : PlayPublishTaskBase() {
    internal fun AndroidPublisher.Edits.updateTracks(
            editId: String,
            inputFolder: File,
            releaseStatus: String,
            versions: List<Long>,
            trackType: String,
            userPercent: Double) {
        val track = tracks()
                .list(variant.applicationId, editId)
                .execute().tracks?.firstOrNull { it.track == trackType }
                ?: Track()

        var releaseText: List<LocalizedText>? = null
        if (inputFolder.exists()) {
            releaseText = inputFolder.listFiles(LocaleFileFilter).map { locale ->
                val fileName = ListingDetail.WHATS_NEW.fileName
                val file = run {
                    var file = File(locale, "$fileName-${extension.track}").orNull()
                    if (file == null) file = File(locale, fileName).orNull()
                    file
                } ?: return@map null
                val recentChanges = File(file, fileName).readProcessed(
                        ListingDetail.WHATS_NEW.maxLength,
                        extension.errorOnSizeLimit
                )
                LocalizedText().setLanguage(locale.name).setText(recentChanges)
            }.filterNotNull()
        }
        val trackRelease = TrackRelease().apply {
            releaseNotes = releaseText
            status = releaseStatus
            userFraction = if (status == ReleaseStatus.INPROGRESS.status) userPercent else 0.0
            versionCodes = versions
        }

        track.releases = listOf(trackRelease)

        tracks()
                .update(variant.applicationId, editId, trackType, track)
                .execute()
    }
}
