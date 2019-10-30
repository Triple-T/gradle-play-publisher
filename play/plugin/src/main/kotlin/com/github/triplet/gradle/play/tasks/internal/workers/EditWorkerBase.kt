package com.github.triplet.gradle.play.tasks.internal.workers

import com.github.triplet.gradle.androidpublisher.EditManager
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.internal.commitOrDefault
import com.google.api.services.androidpublisher.AndroidPublisher
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

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
