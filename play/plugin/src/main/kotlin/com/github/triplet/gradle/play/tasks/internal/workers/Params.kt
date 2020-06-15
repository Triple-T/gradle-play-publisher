package com.github.triplet.gradle.play.tasks.internal.workers

import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.play.internal.toConfig
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishEditTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.UploadArtifactTaskBase

internal fun PublishTaskBase.paramsForBase(params: PlayWorkerBase.PlayPublishingParams) {
    params.config.set(extension.toConfig())
    params.appId.set(appId)

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

    into.mappingFile.set(mappingFile)
}
