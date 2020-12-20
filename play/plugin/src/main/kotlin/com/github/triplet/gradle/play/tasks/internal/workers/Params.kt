package com.github.triplet.gradle.play.tasks.internal.workers

import com.github.triplet.gradle.play.internal.toConfig
import com.github.triplet.gradle.play.tasks.internal.PublishArtifactTaskBase
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase

internal fun PublishTaskBase.paramsForBase(params: PlayWorkerBase.PlayPublishingParams) {
    params.config.set(extension.toConfig())
    params.apiService.set(apiService)

    if (params is PublishArtifactWorkerBase.ArtifactPublishingParams) {
        this as PublishArtifactTaskBase

        params.releaseNotesDir.set(releaseNotesDir)
        params.consoleNamesDir.set(consoleNamesDir)
    }
}

internal fun PlayWorkerBase.PlayPublishingParams.copy(into: PlayWorkerBase.PlayPublishingParams) {
    into.config.set(config)
    into.apiService.set(apiService)
}

internal fun EditWorkerBase.EditPublishingParams.copy(into: EditWorkerBase.EditPublishingParams) {
    (this as PlayWorkerBase.PlayPublishingParams).copy(into)
}

internal fun PublishArtifactWorkerBase.ArtifactPublishingParams.copy(
        into: PublishArtifactWorkerBase.ArtifactPublishingParams
) {
    (this as EditWorkerBase.EditPublishingParams).copy(into)

    into.releaseNotesDir.set(releaseNotesDir)
    into.consoleNamesDir.set(consoleNamesDir)
}
