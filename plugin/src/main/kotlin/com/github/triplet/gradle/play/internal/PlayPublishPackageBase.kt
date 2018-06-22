package com.github.triplet.gradle.play.internal

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

abstract class PlayPublishPackageBase : PlayPublishTaskBase() {
    @get:Internal internal lateinit var resDir: File

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputDirectory
    internal val releaseNotesDir by lazy { File(resDir, RELEASE_NOTES_PATH) }

    protected fun AndroidPublisher.Edits.updateTracks(editId: String, versions: List<Long>) {
        progressLogger.progress("Updating tracks")

        val releaseTexts = releaseNotesDir.listFiles()?.mapNotNull { locale ->
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

        val track = Track().apply {
            track = extension.track
            releases = listOf(trackRelease)
        }
        tracks()
                .update(variant.applicationId, editId, extension.track, track)
                .execute()
    }
}
