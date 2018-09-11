package com.github.triplet.gradle.play.internal

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
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
    internal val releaseNotesDir
        get() = File(resDir, RELEASE_NOTES_PATH).orNull()

    protected fun AndroidPublisher.Edits.updateTracks(editId: String, versions: List<Long>) {
        progressLogger.progress("Updating tracks")

        val releaseTexts = releaseNotesDir?.listFiles()?.mapNotNull { locale ->
            val file = File(locale, "${extension.track}.txt").orNull()
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

    protected fun GoogleJsonResponseException.handleUploadFailures(file: File): Nothing? {
        val isConflict = details?.errors.orEmpty().all {
            it.reason == "apkUpgradeVersionConflict" || it.reason == "apkNoUpgradePath"
        }
        if (isConflict) {
            when (extension._resolutionStrategy) {
                ResolutionStrategy.AUTO -> throw IllegalStateException(
                        "Concurrent uploads for variant ${variant.name}. Make sure to " +
                                "synchronously upload your APKs such that they don't conflict.",
                        this
                )
                ResolutionStrategy.FAIL -> throw IllegalStateException(
                        "Version code ${variant.versionCode} is too low for variant ${variant.name}.",
                        this
                )
                ResolutionStrategy.IGNORE -> logger.warn(
                        "Ignoring APK ($file) for version code ${variant.versionCode}")
            }
            return null
        } else {
            throw this
        }
    }

    protected fun AndroidPublisher.Edits.handlePackageDetails(editId: String, versionCode: Int) {
        val file = variant.mappingFile
        if (file != null && file.length() > 0) {
            val mapping = FileContent(MIME_TYPE_STREAM, file)
            deobfuscationfiles()
                    .upload(variant.applicationId, editId, versionCode, "proguard", mapping)
                    .trackUploadProgress(progressLogger, "mapping file")
                    .execute()
        }
    }
}
