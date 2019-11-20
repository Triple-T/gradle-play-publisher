package com.github.triplet.gradle.play.tasks.internal.workers

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import java.io.File

internal abstract class UploadArtifactWorkerBase<T : UploadArtifactWorkerBase.ArtifactUploadingParams> :
        PublishArtifactWorkerBase<T>() {
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

    internal interface ArtifactUploadingParams : ArtifactPublishingParams {
        val variantName: Property<String>
        val versionCodes: MapProperty<File, Int>

        val mappingFile: RegularFileProperty // Optional
    }
}
