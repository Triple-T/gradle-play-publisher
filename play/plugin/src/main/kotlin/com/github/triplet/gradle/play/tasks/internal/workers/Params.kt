package com.github.triplet.gradle.play.tasks.internal.workers

import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishEditTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase

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
