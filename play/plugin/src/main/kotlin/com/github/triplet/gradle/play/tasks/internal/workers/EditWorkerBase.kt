package com.github.triplet.gradle.play.tasks.internal.workers

import com.github.triplet.gradle.androidpublisher.EditManager
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

internal abstract class EditWorkerBase<T : EditWorkerBase.EditPublishingParams> :
        PlayWorkerBase<T>() {
    protected val editId = parameters.editId.get()
    protected val edits = EditManager(publisher, editId)

    protected fun commit() {
        if (config.commit) {
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
