package com.github.triplet.gradle.play.tasks.internal.workers

import org.gradle.api.file.RegularFileProperty

internal abstract class UploadArtifactWorkerBase<T : UploadArtifactWorkerBase.ArtifactUploadingParams> :
        PublishArtifactWorkerBase<T>() {
    internal interface ArtifactUploadingParams : ArtifactPublishingParams {
        val mappingFile: RegularFileProperty // Optional
    }
}
