package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.MIME_TYPE_STREAM
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.commitOrDefault
import com.github.triplet.gradle.play.internal.has
import com.github.triplet.gradle.play.internal.isRollout
import com.github.triplet.gradle.play.internal.marked
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.readProcessed
import com.github.triplet.gradle.play.internal.releaseStatusOrDefault
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.github.triplet.gradle.play.internal.safeCreateNewFile
import com.github.triplet.gradle.play.internal.trackOrDefault
import com.github.triplet.gradle.play.internal.userFractionOrDefault
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
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import kotlin.math.roundToInt

internal fun PublishTaskBase.paramsForBase(params: PlayWorkerBase.PlayPublishingParams) {
    params.config.set(extension.serializableConfig)
    params.appId.set(variant.applicationId)

    if (params is EditWorkerBase.EditPublishingParams) {
        this as PublishEditTaskBase

        params.editId.set(editId)
        params.commitMarker.set(editIdFile.asFile.get().marked("commit"))
        params.skippedMarker.set(editIdFile.asFile.get().marked("skipped"))
    }

    if (params is ArtifactWorkerBase.ArtifactPublishingParams) {
        this as PublishArtifactTaskBase

        params.variantName.set(variant.name)
        params.versionCode.set(variant.outputs.map { it.versionCode }.first())

        params.releaseNotesDir.set(releaseNotesDir)
        params.consoleNamesDir.set(consoleNamesDir)
        params.transientConsoleName.set(releaseName)
        params.mappingFile.set(mappingFile)
    }
}

internal fun PlayWorkerBase.PlayPublishingParams.copy(into: PlayWorkerBase.PlayPublishingParams) {
    into.config.set(config.get())
    into.appId.set(appId.get())
}

internal fun EditWorkerBase.EditPublishingParams.copy(into: EditWorkerBase.EditPublishingParams) {
    (this as PlayWorkerBase.PlayPublishingParams).copy(into)

    into.editId.set(editId.get())
    into.commitMarker.set(commitMarker.get())
    into.skippedMarker.set(skippedMarker.get())
}

internal fun ArtifactWorkerBase.ArtifactPublishingParams.copy(
        into: ArtifactWorkerBase.ArtifactPublishingParams
) {
    (this as EditWorkerBase.EditPublishingParams).copy(into)

    into.variantName.set(variantName.get())
    into.versionCode.set(versionCode.get())

    into.releaseNotesDir.set(releaseNotesDir.orNull)
    into.consoleNamesDir.set(consoleNamesDir.orNull)
    into.transientConsoleName.set(transientConsoleName.orNull)
    into.mappingFile.set(mappingFile.orNull)
}

internal abstract class PlayWorkerBase<T : PlayWorkerBase.PlayPublishingParams> : WorkAction<T> {
    protected val config = parameters.config.get()
    protected val appId = parameters.appId.get()

    protected val publisher = config.buildPublisher()
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

    internal interface PlayPublishingParams : WorkParameters {
        val config: Property<PlayPublisherExtension.Config>
        val appId: Property<String>
    }
}

internal abstract class EditWorkerBase<T : EditWorkerBase.EditPublishingParams> :
        PlayWorkerBase<T>() {
    protected val editId = parameters.editId.get()
    protected val edits: AndroidPublisher.Edits = publisher.edits()

    protected fun commit() {
        if (config.commitOrDefault) {
            parameters.commitMarker.get().safeCreateNewFile()
        } else {
            parameters.skippedMarker.get().safeCreateNewFile()
        }
    }

    internal interface EditPublishingParams : PlayPublishingParams {
        val editId: Property<String>
        val commitMarker: Property<File>
        val skippedMarker: Property<File>
    }
}

internal abstract class ArtifactWorkerBase<T : ArtifactWorkerBase.ArtifactPublishingParams> :
        EditWorkerBase<T>() {
    protected var commit = true

    final override fun execute() {
        upload()
        if (commit) commit()
    }

    abstract fun upload()

    protected fun updateTracks(versions: List<Long>) {
        val track = if (parameters.skippedMarker.get().exists()) {
            createTrackForSkippedCommit(versions)
        } else if (config.releaseStatusOrDefault.isRollout()) {
            createTrackForRollout(versions)
        } else {
            createDefaultTrack(versions)
        }

        println("Updating ${track.releases.map { it.status }.distinct()} release " +
                        "($appId:${track.releases.flatMap { it.versionCodes.orEmpty() }}) " +
                        "in track '${track.track}'")
        edits.tracks().update(appId, editId, config.trackOrDefault, track).execute()
    }

    protected fun TrackRelease.applyChanges(
            versionCodes: List<Long>? = null,
            updateStatus: Boolean = true,
            updateFraction: Boolean = true,
            updateConsoleName: Boolean = true
    ): TrackRelease {
        versionCodes?.let { updateVersionCodes(it) }
        if (updateStatus) updateStatus()
        if (updateConsoleName) updateConsoleName()
        maybeUpdateReleaseNotes()
        if (updateFraction) updateUserFraction()

        return this
    }

    protected fun handleUploadFailures(
            e: GoogleJsonResponseException,
            file: File
    ): Nothing? = if (e has "apkUpgradeVersionConflict" || e has "apkNoUpgradePath") {
        when (config.resolutionStrategyOrDefault) {
            ResolutionStrategy.AUTO -> throw IllegalStateException(
                    "Concurrent uploads for variant ${parameters.variantName.get()} (version " +
                            "code ${parameters.versionCode.get()} already used). Make sure to " +
                            "synchronously upload your APKs such that they don't conflict. If " +
                            "this problem persists, delete your drafts in the Play Console's " +
                            "artifact library.",
                    e
            )
            ResolutionStrategy.FAIL -> throw IllegalStateException(
                    "Version code ${parameters.versionCode.get()} is too low or has already been " +
                            "used for variant ${parameters.variantName.get()}.",
                    e
            )
            ResolutionStrategy.IGNORE -> println(
                    "Ignoring artifact ($file) for version code ${parameters.versionCode.get()}")
        }
        null
    } else {
        throw e
    }

    protected fun uploadMappingFile(versionCode: Int) {
        val file = parameters.mappingFile.orNull
        if (file != null && file.length() > 0) {
            val mapping = FileContent(MIME_TYPE_STREAM, file)
            edits.deobfuscationfiles()
                    .upload(appId, editId, versionCode, "proguard", mapping)
                    .trackUploadProgress("mapping file", file)
                    .execute()
        }
    }

    private fun createTrackForSkippedCommit(versions: List<Long>): Track {
        val track = edits.tracks().get(appId, editId, config.trackOrDefault).execute()

        track.releases = if (track.releases.isNullOrEmpty()) {
            listOf(TrackRelease().applyChanges(versions))
        } else {
            track.releases.map {
                if (it.status == config.releaseStatusOrDefault.publishedName) {
                    it.applyChanges(it.versionCodes.orEmpty() + versions)
                } else {
                    it
                }
            }
        }

        return track
    }

    private fun createTrackForRollout(versions: List<Long>): Track {
        val track = edits.tracks().get(appId, editId, config.trackOrDefault).execute()

        val keep = track.releases.orEmpty().filterNot(TrackRelease::isRollout)
        track.releases = keep + listOf(TrackRelease().applyChanges(versions))

        return track
    }

    private fun createDefaultTrack(versions: List<Long>) = Track().apply {
        track = config.trackOrDefault
        releases = listOf(TrackRelease().applyChanges(versions))
    }

    private fun TrackRelease.updateVersionCodes(it: List<Long>) {
        this.versionCodes = it + config.retain.artifacts.orEmpty()
    }

    private fun TrackRelease.updateStatus() {
        status = config.releaseStatusOrDefault.publishedName
    }

    private fun TrackRelease.updateConsoleName() {
        name = if (parameters.transientConsoleName.isPresent) {
            parameters.transientConsoleName.get()
        } else if (parameters.consoleNamesDir.isPresent) {
            val dir = parameters.consoleNamesDir.get()
            val file = File(dir, "${config.trackOrDefault}.txt").orNull()
                    ?: File(dir, RELEASE_NAMES_DEFAULT_NAME).orNull()

            file?.readProcessed()?.lines()?.firstOrNull()
        } else {
            null
        }
    }

    private fun TrackRelease.maybeUpdateReleaseNotes() {
        val locales = parameters.releaseNotesDir.orNull?.listFiles().orEmpty()
        val releaseNotes = locales.mapNotNull { locale ->
            val file = File(locale, "${config.trackOrDefault}.txt").orNull() ?: run {
                File(locale, RELEASE_NOTES_DEFAULT_NAME).orNull() ?: return@mapNotNull null
            }

            LocalizedText().apply {
                language = locale.name
                text = file.readProcessed()
            }
        }

        if (releaseNotes.isNotEmpty()) updateReleaseNotes(releaseNotes)
    }

    private fun TrackRelease.updateReleaseNotes(releaseNotes: List<LocalizedText>) {
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

    private fun TrackRelease.updateUserFraction() {
        userFraction = config.userFractionOrDefault.takeIf { isRollout() }
    }

    internal interface ArtifactPublishingParams : EditPublishingParams {
        val variantName: Property<String>
        val versionCode: Property<Int>

        val releaseNotesDir: Property<File?>
        val consoleNamesDir: Property<File?>
        val transientConsoleName: Property<String?>
        val mappingFile: Property<File?>
    }
}
