package com.github.triplet.gradle.play.tasks.internal.workers

import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishEditTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.UploadArtifactTaskBase

internal fun PublishTaskBase.paramsForBase(params: PlayWorkerBase.PlayPublishingParams) {
    params.config.set(extension.serializableConfig)
    params.appId.set(variant.applicationId)

    if (params is EditWorkerBase.EditPublishingParams) {
        this as PublishEditTaskBase

        params.editId.set(editId)
        params.commitMarker.set(editIdFile.get().asFile.marked("commit"))
        params.skippedMarker.set(editIdFile.get().asFile.marked("skipped"))
    }

    if (params is PublishArtifactWorkerBase.ArtifactPublishingParams) {
        this as PublishArtifactTaskBase

        params.releaseNotesDir.set(releaseNotesDir)
        params.consoleNamesDir.set(consoleNamesDir)
    }

    if (params is UploadArtifactWorkerBase.ArtifactUploadingParams) {
        this as UploadArtifactTaskBase

        params.variantName.set(variant.name)
        params.versionCodes.set(variant.outputs.associate { it.outputFile to it.versionCode })

        params.mappingFile.set(mappingFile)
    }
}

internal fun PlayWorkerBase.PlayPublishingParams.copy(into: PlayWorkerBase.PlayPublishingParams) {
    into.config.set(config)
    into.appId.set(appId)
}

internal fun EditWorkerBase.EditPublishingParams.copy(into: EditWorkerBase.EditPublishingParams) {
    (this as PlayWorkerBase.PlayPublishingParams).copy(into)

    into.editId.set(editId)
    into.commitMarker.set(commitMarker)
    into.skippedMarker.set(skippedMarker)
}

internal fun PublishArtifactWorkerBase.ArtifactPublishingParams.copy(
        into: PublishArtifactWorkerBase.ArtifactPublishingParams
) {
    (this as EditWorkerBase.EditPublishingParams).copy(into)

    into.releaseNotesDir.set(releaseNotesDir)
    into.consoleNamesDir.set(consoleNamesDir)
}

internal fun UploadArtifactWorkerBase.ArtifactUploadingParams.copy(
        into: UploadArtifactWorkerBase.ArtifactUploadingParams
) {
    (this as PublishArtifactWorkerBase.ArtifactPublishingParams).copy(into)

    into.variantName.set(variantName)
    into.versionCodes.set(versionCodes.get())

    into.mappingFile.set(mappingFile)
}
