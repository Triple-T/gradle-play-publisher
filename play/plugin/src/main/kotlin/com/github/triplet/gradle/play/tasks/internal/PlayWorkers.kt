package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.androidpublisher.EditManager
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.common.utils.readProcessed
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.commitOrDefault
import com.github.triplet.gradle.play.internal.trackOrDefault
import com.google.api.services.androidpublisher.AndroidPublisher
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File

internal fun PublishTaskBase.paramsForBase(params: PlayWorkerBase.PlayPublishingParams) {
    params.config.set(extension.serializableConfig)
    params.appId.set(variant.applicationId)

    if (params is EditWorkerBase.EditPublishingParams) {
        this as PublishEditTaskBase

        params.editId.set(editId)
        params.commitMarker.set(editIdFile.get().asFile.marked("commit"))
        params.skippedMarker.set(editIdFile.get().asFile.marked("skipped"))
    }

    if (params is ArtifactWorkerBase.ArtifactPublishingParams) {
        this as PublishArtifactTaskBase

        params.variantName.set(variant.name)
        params.versionCodes.set(variant.outputs.associate { it.outputFile to it.versionCode })

        params.releaseNotesDir.set(releaseNotesDir)
        params.consoleNamesDir.set(consoleNamesDir)
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
    into.commitMarker.set(commitMarker)
    into.skippedMarker.set(skippedMarker)
}

internal fun ArtifactWorkerBase.ArtifactPublishingParams.copy(
        into: ArtifactWorkerBase.ArtifactPublishingParams
) {
    (this as EditWorkerBase.EditPublishingParams).copy(into)

    into.variantName.set(variantName.get())
    into.versionCodes.set(versionCodes.get())

    into.releaseNotesDir.set(releaseNotesDir)
    into.consoleNamesDir.set(consoleNamesDir)
    into.mappingFile.set(mappingFile)
}

internal abstract class PlayWorkerBase<T : PlayWorkerBase.PlayPublishingParams> : WorkAction<T> {
    protected val config = parameters.config.get()
    protected val appId = parameters.appId.get()

    protected val publisher by lazy { config.buildPublisher() }
    protected val publisher2 = PlayPublisher(
            config.serviceAccountCredentials!!,
            config.serviceAccountEmail,
            appId
    )

    internal interface PlayPublishingParams : WorkParameters {
        val config: Property<PlayPublisherExtension.Config>
        val appId: Property<String>
    }
}

internal abstract class EditWorkerBase<T : EditWorkerBase.EditPublishingParams> :
        PlayWorkerBase<T>() {
    protected val editId = parameters.editId.get()
    protected val edits: AndroidPublisher.Edits by lazy { publisher.edits() }
    protected val edits2 = EditManager(publisher2, editId)

    protected fun commit() {
        if (config.commitOrDefault) {
            parameters.commitMarker.get().asFile.safeCreateNewFile()
        } else {
            parameters.skippedMarker.get().asFile.safeCreateNewFile()
        }
    }

    internal interface EditPublishingParams : PlayPublishingParams {
        val editId: Property<String>
        val commitMarker: RegularFileProperty
        val skippedMarker: RegularFileProperty
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

    protected fun findBestVersionCode(artifact: File): Long {
        var onTheFlyBuild = parameters.versionCodes.get()[artifact]?.toLong()
        if (onTheFlyBuild == null) {
            // Since we aren't building the supplied artifact, we have no way of knowing its
            // version code without opening it up. Since we don't want to do that, we instead
            // pretend like we know the version code even though we really don't.
            onTheFlyBuild = parameters.versionCodes.get().values.first().toLong()
        }
        return onTheFlyBuild
    }

    protected fun findReleaseName(): String? {
        return if (config.releaseName != null) {
            config.releaseName
        } else if (parameters.consoleNamesDir.isPresent) {
            val dir = parameters.consoleNamesDir.get()
            val file = dir.file("${config.trackOrDefault}.txt").asFile.orNull()
                    ?: dir.file(RELEASE_NAMES_DEFAULT_NAME).asFile.orNull()

            file?.readProcessed()?.lines()?.firstOrNull()
        } else {
            null
        }
    }

    protected fun findReleaseNotes(): Map<String, String?> {
        val locales = parameters.releaseNotesDir.orNull?.asFile?.listFiles().orEmpty()
        return locales.mapNotNull { locale ->
            var result = File(locale, "${config.trackOrDefault}.txt").orNull()
            if (result == null) result = File(locale, RELEASE_NOTES_DEFAULT_NAME).orNull()
            result
        }.associate { notes ->
            notes.parentFile.name to notes.readProcessed()
        }.toSortedMap()
    }

    internal interface ArtifactPublishingParams : EditPublishingParams {
        val variantName: Property<String>
        val versionCodes: Property<Map<File, Int>>

        val releaseNotesDir: DirectoryProperty // Optional
        val consoleNamesDir: DirectoryProperty // Optional
        val mappingFile: RegularFileProperty // Optional
    }
}
