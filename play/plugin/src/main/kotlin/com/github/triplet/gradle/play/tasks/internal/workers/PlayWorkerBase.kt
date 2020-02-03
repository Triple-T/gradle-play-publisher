package com.github.triplet.gradle.play.tasks.internal.workers

import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

internal abstract class PlayWorkerBase<T : PlayWorkerBase.PlayPublishingParams> : WorkAction<T> {
    protected val config = parameters.config.get()
    protected val appId = parameters.appId.get()

    protected val publisher = PlayPublisher(
            config.serviceAccountCredentials!!,
            config.serviceAccountEmail,
            appId
    )

    internal interface PlayPublishingParams : WorkParameters {
        val config: Property<PlayPublisherExtension.Config>
        val appId: Property<String>
    }
}
