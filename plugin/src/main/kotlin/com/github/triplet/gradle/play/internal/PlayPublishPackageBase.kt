package com.github.triplet.gradle.play.internal

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
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

        val releaseTexts = resDir.orNull()?.listFiles()?.filter {
            it.isDirectory
        }?.mapNotNull { locale ->
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

    protected fun GoogleJsonResponseException.handleUploadFailures(file: File): Nothing? {
        val isConflict = details.errors.all {
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
        if (extension.untrackOld && extension._track != TrackType.INTERNAL) {
            progressLogger.progress("Removing old tracks")
            extension._track.superiors.map { it.publishedName }.forEach { channel ->
                try {
                    val track = tracks().get(
                            variant.applicationId,
                            editId,
                            channel
                    ).execute().apply {
                        releases.forEach {
                            it.versionCodes =
                                    it.versionCodes.filter { it > versionCode.toLong() }
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
            val mapping = FileContent(MIME_TYPE_STREAM, variant.mappingFile)
            deobfuscationfiles()
                    .upload(variant.applicationId, editId, versionCode, "proguard", mapping)
                    .trackUploadProgress(progressLogger, "mapping file")
                    .execute()
        }
    }
}
