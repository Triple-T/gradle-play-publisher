package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.internal.MIME_TYPE_STREAM
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_MAX_LENGTH
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_MAX_LENGTH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.has
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.readProcessed
import com.github.triplet.gradle.play.internal.releaseStatusOrDefault
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.github.triplet.gradle.play.internal.trackUploadProgress
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
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
    @get:InputFile
    protected val mappingFile: File?
        get() {
            val customDir = extension._artifactDir

            return if (customDir == null) {
                variant.mappingFile.orNull()
            } else {
                customDir.listFiles().orEmpty().singleOrNull { it.name == "mapping.txt" }
            }
        }

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputDirectory
    protected val releaseNotesDir
        get() = File(resDir, RELEASE_NOTES_PATH).orNull()

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputDirectory
    protected val consoleNamesDir
        get() = File(resDir, RELEASE_NAMES_PATH).orNull()

    protected fun AndroidPublisher.Edits.updateTracks(editId: String, versions: List<Long>) {
        progressLogger.progress("Updating tracks")

        val track = if (hasSavedEdit) {
            tracks().get(variant.applicationId, editId, extension.track).execute().apply {
                releases = if (releases.isNullOrEmpty()) {
                    listOf(TrackRelease().applyChanges(versions))
                } else {
                    releases.map {
                        if (it.status == extension.releaseStatusOrDefault.publishedName) {
                            it.applyChanges(it.versionCodes.orEmpty() + versions)
                        } else {
                            it
                        }
                    }
                }
            }
        } else if (extension.releaseStatusOrDefault == ReleaseStatus.IN_PROGRESS) {
            tracks().get(variant.applicationId, editId, extension.track).execute().apply {
                val keep = releases.orEmpty().filter {
                    it.status == ReleaseStatus.COMPLETED.publishedName ||
                            it.status == ReleaseStatus.DRAFT.publishedName
                }
                releases = keep + listOf(TrackRelease().applyChanges(versions))
            }
        } else {
            Track().apply {
                track = extension.track
                releases = listOf(TrackRelease().applyChanges(versions))
            }
        }

        tracks()
                .update(variant.applicationId, editId, extension.track, track)
                .execute()
    }

    protected fun TrackRelease.applyChanges(
            versionCodes: List<Long>? = null,
            updateStatus: Boolean = true,
            updateFraction: Boolean = true,
            updateConsoleName: Boolean = true
    ): TrackRelease {
        versionCodes?.let { this.versionCodes = it }
        if (updateStatus) status = extension.releaseStatus
        if (updateConsoleName) {
            val file = File(consoleNamesDir, "${extension.track}.txt").orNull()
                    ?: File(consoleNamesDir, RELEASE_NAMES_DEFAULT_NAME).orNull()
            name = file?.readProcessed(RELEASE_NAMES_MAX_LENGTH)?.lines()?.firstOrNull()
        }

        val releaseNotes = releaseNotesDir?.listFiles().orEmpty().mapNotNull { locale ->
            val file = File(locale, "${extension.track}.txt").orNull() ?: run {
                File(locale, RELEASE_NOTES_DEFAULT_NAME).orNull() ?: return@mapNotNull null
            }

            LocalizedText().apply {
                language = locale.name
                text = file.readProcessed(RELEASE_NOTES_MAX_LENGTH)
            }
        }
        if (releaseNotes.isNotEmpty()) {
            val existingReleaseNotes = this.releaseNotes.orEmpty()
            this.releaseNotes = if (existingReleaseNotes.isEmpty()) {
                releaseNotes
            } else {
                val merged = releaseNotes.toMutableList()

                for (existing in existingReleaseNotes) {
                    if (merged.none { it.language == existing.language }) merged += existing
                }

                merged
            }
        }

        if (updateFraction) {
            val status = extension.releaseStatus
            userFraction = if (
                    status == ReleaseStatus.IN_PROGRESS.publishedName ||
                    status == ReleaseStatus.HALTED.publishedName
            ) extension.userFraction else null
        }

        return this
    }

    protected fun handleUploadFailures(e: GoogleJsonResponseException, file: File): Nothing? {
        val isConflict = e has "apkUpgradeVersionConflict" || e has "apkNoUpgradePath"
        if (isConflict) {
            when (extension.resolutionStrategyOrDefault) {
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
                        "Ignoring APK ($file) for version code ${variant.versionCode}")
            }
            return null
        } else {
            throw e
        }
    }

    protected fun AndroidPublisher.Edits.handlePackageDetails(editId: String, versionCode: Int) {
        val file = mappingFile
        if (file != null && file.length() > 0) {
            val mapping = FileContent(MIME_TYPE_STREAM, file)
            deobfuscationfiles()
                    .upload(variant.applicationId, editId, versionCode, "proguard", mapping)
                    .trackUploadProgress(progressLogger, "mapping file")
                    .execute()
        }
    }
}
