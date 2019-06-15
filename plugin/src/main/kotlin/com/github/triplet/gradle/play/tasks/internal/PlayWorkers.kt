package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.MIME_TYPE_STREAM
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_MAX_LENGTH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_MAX_LENGTH
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.has
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.readProcessed
import com.github.triplet.gradle.play.internal.releaseStatusOrDefault
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherRequest
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.workers.WorkerConfiguration
import java.io.File
import java.io.Serializable
import kotlin.math.roundToInt

internal fun PlayPublishTaskBase.paramsForBase(
        config: WorkerConfiguration,
        p: Any,
        editId: String? = null
) {
    val base = PlayWorkerBase.PlayPublishingData(
            extension.toSerializable(),
            variant.applicationId,
            savedEditId,
            editId
    )

    if (this is PlayPublishArtifactBase) {
        val artifact = ArtifactWorkerBase.ArtifactPublishingData(
                variant.name,
                variant.outputs.map { it.versionCode }.first(),

                releaseNotesDir,
                consoleNamesDir,
                mappingFile
        )

        config.params(p, artifact, base)
    } else {
        config.params(p, base)
    }
}

internal abstract class PlayWorkerBase(private val data: PlayPublishingData) : Runnable {
    protected val extension = data.extension
    protected val publisher = extension.buildPublisher()
    protected val appId = data.applicationId
    protected val editId by lazy {
        data.editId ?: publisher.getOrCreateEditId(appId, data.savedEditId)
    }
    protected val edits: AndroidPublisher.Edits = publisher.edits()
    protected val logger: Logger = Logging.getLogger(Task::class.java)

    protected fun commit() = publisher.commit(extension, appId, editId, data.savedEditId)

    protected fun <T> AndroidPublisherRequest<T>.trackUploadProgress(
            thing: String
    ): AndroidPublisherRequest<T> {
        mediaHttpUploader?.setProgressListener {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (it.uploadState) {
                MediaHttpUploader.UploadState.INITIATION_STARTED ->
                    println("Starting $thing upload")
                MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS ->
                    println("Uploading $thing: ${(it.progress * 100).roundToInt()}% complete")
                MediaHttpUploader.UploadState.MEDIA_COMPLETE ->
                    println("${thing.capitalize()} upload complete")
            }
        }
        return this
    }

    internal data class PlayPublishingData(
            val extension: PlayPublisherExtension.Serializable,

            val applicationId: String,

            val savedEditId: File?,
            val editId: String?
    ) : Serializable
}

internal abstract class ArtifactWorkerBase(
        private val artifact: ArtifactPublishingData,
        private val play: PlayPublishingData
) : PlayWorkerBase(play) {
    final override fun run() {
        upload()
        commit()
    }

    abstract fun upload()

    protected fun updateTracks(editId: String, versions: List<Long>) {
        val track = if (play.savedEditId?.orNull() != null) {
            edits.tracks().get(appId, editId, extension.track).execute().apply {
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
            edits.tracks().get(appId, editId, extension.track).execute().apply {
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

        println("Updating ${track.releases.map { it.status }.distinct()} release " +
                        "($appId:${track.releases.flatMap { it.versionCodes.orEmpty() }}) " +
                        "in track '${track.track}'")
        edits.tracks().update(appId, editId, extension.track, track).execute()
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
            val file = File(artifact.consoleNamesDir, "${extension.track}.txt").orNull()
                    ?: File(artifact.consoleNamesDir, RELEASE_NAMES_DEFAULT_NAME).orNull()
            name = file?.readProcessed(RELEASE_NAMES_MAX_LENGTH)?.lines()?.firstOrNull()
        }

        val releaseNotes = artifact.releaseNotesDir?.listFiles().orEmpty().mapNotNull { locale ->
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

    protected fun handleUploadFailures(
            e: GoogleJsonResponseException,
            file: File
    ): Nothing? = if (e has "apkUpgradeVersionConflict" || e has "apkNoUpgradePath") {
        when (extension.resolutionStrategyOrDefault) {
            ResolutionStrategy.AUTO -> throw IllegalStateException(
                    "Concurrent uploads for variant ${artifact.variantName} (version code " +
                            "${artifact.versionCode} already used). Make sure to synchronously " +
                            "upload your APKs such that they don't conflict. If this problem " +
                            "persists, delete your drafts in the Play Console's artifact library.",
                    e
            )
            ResolutionStrategy.FAIL -> throw IllegalStateException(
                    "Version code ${artifact.versionCode} is too low or has already been used " +
                            "for variant ${artifact.variantName}.",
                    e
            )
            ResolutionStrategy.IGNORE -> println(
                    "Ignoring artifact ($file) for version code ${artifact.versionCode}")
        }
        null
    } else {
        throw e
    }

    protected fun handleArtifactDetails(editId: String, versionCode: Int) {
        val file = artifact.mappingFile
        if (file != null && file.length() > 0) {
            val mapping = FileContent(MIME_TYPE_STREAM, file)
            edits.deobfuscationfiles()
                    .upload(appId, editId, versionCode, "proguard", mapping)
                    .trackUploadProgress("mapping file")
                    .execute()
        }
    }

    internal data class ArtifactPublishingData(
            val variantName: String,
            val versionCode: Int,

            val releaseNotesDir: File?,
            val consoleNamesDir: File?,
            val mappingFile: File?
    ) : Serializable
}
