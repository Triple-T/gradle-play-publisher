package com.github.triplet.gradle.androidpublisher

import com.github.triplet.gradle.androidpublisher.internal.DefaultPlayPublisher
import com.google.common.annotations.VisibleForTesting
import java.io.File

interface PlayPublisher {
    /**
     * Uploads the given [bundleFile] as an Internal Sharing artifact.
     *
     * More docs
     * [here](https://developers.google.com/android-publisher/api-ref/internalappsharingartifacts/uploadbundle).
     */
    fun uploadInternalSharingBundle(bundleFile: File): UploadInternalSharingArtifactResponse

    /**
     * Uploads the given [apkFile] as an Internal Sharing artifact.
     *
     * More docs
     * [here](https://developers.google.com/android-publisher/api-ref/internalappsharingartifacts/uploadapk).
     */
    fun uploadInternalSharingApk(apkFile: File): UploadInternalSharingArtifactResponse

    /**
     * Uploads the given [productFile]. If it doesn't yet exist, it will be created. Otherwise, it
     * will be updated.
     *
     * More docs [here](https://developers.google.com/android-publisher/api-ref/inappproducts).
     */
    fun publishInAppProduct(productFile: File)

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
