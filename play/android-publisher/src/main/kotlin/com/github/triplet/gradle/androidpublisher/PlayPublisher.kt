package com.github.triplet.gradle.androidpublisher

import com.github.triplet.gradle.androidpublisher.internal.DefaultPlayPublisher
import com.google.api.services.androidpublisher.model.InAppProduct
import com.google.common.annotations.VisibleForTesting
import java.io.File

interface PlayPublisher {
    fun uploadInternalSharingBundle(bundleFile: File): String

    fun publishInAppProduct(product: InAppProduct)

    interface Factory {
        fun create(
                credentials: File,
                email: String?,
                appId: String
        ): PlayPublisher
    }

    companion object {
        private var factory: Factory = DefaultPlayPublisher

        @VisibleForTesting
        fun setFactory(factory: Factory) {
            Companion.factory = factory
        }

        operator fun invoke(
                credentials: File,
                email: String?,
                appId: String
        ): PlayPublisher = factory.create(credentials, email, appId)
    }
}
