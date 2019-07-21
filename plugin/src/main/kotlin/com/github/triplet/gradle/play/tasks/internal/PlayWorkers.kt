package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.MIME_TYPE_STREAM
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.has
import com.github.triplet.gradle.play.internal.marked
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.readProcessed
import com.github.triplet.gradle.play.internal.releaseStatusOrDefault
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.github.triplet.gradle.play.internal.safeCreateNewFile
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

internal fun PlayPublishTaskBase.paramsForBase(config: WorkerConfiguration, p: Any) {
    val base = PlayWorkerBase.PlayPublishingParams(
            extension.toSerializable(),
            variant.applicationId
    )

    if (this is PlayPublishEditTaskBase) {
        val edit = EditWorkerBase.EditPublishingParams(
                editId,
                editIdFile.asFile.get().marked("commit"),
                editIdFile.asFile.get().marked("skipped"),

                base
        )

        if (this is PlayPublishArtifactBase) {
            val artifact = ArtifactWorkerBase.ArtifactPublishingParams(
                    variant.name,
                    variant.outputs.map { it.versionCode }.first(),

                    releaseNotesDir,
                    consoleNamesDir,
                    releaseName,
                    mappingFile,

                    edit
            )
            config.params(p, artifact)
        } else {
            config.params(p, edit)
        }
    } else {
        config.params(p, base)
    }
}

internal abstract class PlayWorkerBase(p: PlayPublishingParams) : Runnable {
    protected val extension = p.extension
    protected val appId = p.appId

    protected val publisher = extension.buildPublisher()
    protected val logger: Logger = Logging.getLogger(Task::class.java)

    protected fun <T> AndroidPublisherRequest<T>.trackUploadProgress(
            thing: String,
            file: File
    ): AndroidPublisherRequest<T> {
        mediaHttpUploader?.setProgressListener {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (it.uploadState) {
                MediaHttpUploader.UploadState.INITIATION_STARTED ->
                    println("Starting $thing upload: $file")
                MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS ->
                    println("Uploading $thing: ${(it.progress * 100).roundToInt()}% complete")
                MediaHttpUploader.UploadState.MEDIA_COMPLETE ->
                    println("${thing.capitalize()} upload complete")
            }
        }
        return this
    }

    internal data class PlayPublishingParams(
            val extension: PlayPublisherExtension.Serializable,
            val appId: String
    ) : Serializable
}

internal abstract class EditWorkerBase(
        private val p: EditPublishingParams
) : PlayWorkerBase(p.base) {
    protected val editId = p.editId
    protected val edits: AndroidPublisher.Edits = publisher.edits()

    protected fun commit() {
        (if (extension.commit) p.commitMarker else p.skippedMarker).safeCreateNewFile()
    }

    internal data class EditPublishingParams(
            val editId: String,
            val commitMarker: File,
            val skippedMarker: File,

            val base: PlayPublishingParams
    ) : Serializable
}

internal abstract class ArtifactWorkerBase(
        private val p: ArtifactPublishingParams
) : EditWorkerBase(p.base) {
    protected var commit = true

    final override fun run() {
        upload()
        if (commit) commit()
    }

    abstract fun upload()

    protected fun updateTracks(editId: String, versions: List<Long>) {
        val track = if (p.base.skippedMarker.exists()) {
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
            name = if (p.transientConsoleName == null) {
                val file = File(p.consoleNamesDir, "${extension.track}.txt").orNull()
                        ?: File(p.consoleNamesDir, RELEASE_NAMES_DEFAULT_NAME).orNull()
                file?.readProcessed()?.lines()?.firstOrNull()
            } else {
                p.transientConsoleName
            }
        }

        val releaseNotes = p.releaseNotesDir?.listFiles().orEmpty().mapNotNull { locale ->
            val file = File(locale, "${extension.track}.txt").orNull() ?: run {
                File(locale, RELEASE_NOTES_DEFAULT_NAME).orNull() ?: return@mapNotNull null
            }

            LocalizedText().apply {
                language = locale.name
                text = file.readProcessed()
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
            userFraction = extension.userFraction.takeIf {
                status == ReleaseStatus.IN_PROGRESS.publishedName ||
                        status == ReleaseStatus.HALTED.publishedName
            }
        }

        return this
    }

    protected fun handleUploadFailures(
            e: GoogleJsonResponseException,
            file: File
    ): Nothing? = if (e has "apkUpgradeVersionConflict" || e has "apkNoUpgradePath") {
        when (extension.resolutionStrategyOrDefault) {
            ResolutionStrategy.AUTO -> throw IllegalStateException(
                    "Concurrent uploads for variant ${p.variantName} (version code " +
                            "${p.versionCode} already used). Make sure to synchronously " +
                            "upload your APKs such that they don't conflict. If this problem " +
                            "persists, delete your drafts in the Play Console's artifact library.",
                    e
            )
            ResolutionStrategy.FAIL -> throw IllegalStateException(
                    "Version code ${p.versionCode} is too low or has already been used " +
                            "for variant ${p.variantName}.",
                    e
            )
            ResolutionStrategy.IGNORE -> println(
                    "Ignoring artifact ($file) for version code ${p.versionCode}")
        }
        null
    } else {
        throw e
    }

    protected fun handleArtifactDetails(editId: String, versionCode: Int) {
        val file = p.mappingFile
        if (file != null && file.length() > 0) {
            val mapping = FileContent(MIME_TYPE_STREAM, file)
            edits.deobfuscationfiles()
                    .upload(appId, editId, versionCode, "proguard", mapping)
                    .trackUploadProgress("mapping file", file)
                    .execute()
        }
    }

    internal data class ArtifactPublishingParams(
            val variantName: String,
            val versionCode: Int,

            val releaseNotesDir: File?,
            val consoleNamesDir: File?,
            val transientConsoleName: String?,
            val mappingFile: File?,

            val base: EditPublishingParams
    ) : Serializable
}
