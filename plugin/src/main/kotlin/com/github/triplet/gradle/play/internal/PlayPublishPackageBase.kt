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
    internal lateinit var releaseNotesDir: File

    protected fun AndroidPublisher.Edits.updateTracks(editId: String, versions: List<Long>) {
        val track = tracks()
                .list(variant.applicationId, editId)
                .execute().tracks
                ?.firstOrNull { it.track == extension.track } ?: Track()

        val releaseTexts = releaseNotesDir.orNull()?.listFiles()?.mapNotNull { locale ->
            val file = File(locale, extension.track).orNull()
                    ?: File(locale, RELEASE_NOTES_DEFAULT_NAME).orNull()
                    ?: return@mapNotNull null

            LocalizedText().apply {
                language = locale.name
                text = file.readProcessed(RELEASE_NOTES_MAX_LENGTH)
            }
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
