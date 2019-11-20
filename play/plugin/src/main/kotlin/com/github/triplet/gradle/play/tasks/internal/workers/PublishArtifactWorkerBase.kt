package com.github.triplet.gradle.play.tasks.internal.workers

import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.common.utils.readProcessed
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_DEFAULT_NAME
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_DEFAULT_NAME
import org.gradle.api.file.DirectoryProperty
import java.io.File

internal abstract class PublishArtifactWorkerBase<T : PublishArtifactWorkerBase.ArtifactPublishingParams> :
        EditWorkerBase<T>() {
    protected var commit = true

    final override fun execute() {
        upload()
        if (commit) commit()
    }

    abstract fun upload()

    protected fun findReleaseName(track: String): String? {
        return if (config.releaseName != null) {
            config.releaseName
        } else if (parameters.consoleNamesDir.isPresent) {
            val dir = parameters.consoleNamesDir.get()
            val file = dir.file("$track.txt").asFile.orNull()
                    ?: dir.file(RELEASE_NAMES_DEFAULT_NAME).asFile.orNull()

            file?.readProcessed()?.lines()?.firstOrNull()
        } else {
            null
        }
    }

    protected fun findReleaseNotes(track: String): Map<String, String?> {
        val locales = parameters.releaseNotesDir.orNull?.asFile?.listFiles().orEmpty()
        return locales.mapNotNull { locale ->
            var result = File(locale, "$track.txt").orNull()
            if (result == null) result = File(locale, RELEASE_NOTES_DEFAULT_NAME).orNull()
            result
        }.associate { notes ->
            notes.parentFile.name to notes.readProcessed()
        }.toSortedMap()
    }

    internal interface ArtifactPublishingParams : EditPublishingParams {
        val releaseNotesDir: DirectoryProperty // Optional
        val consoleNamesDir: DirectoryProperty // Optional
    }
}
