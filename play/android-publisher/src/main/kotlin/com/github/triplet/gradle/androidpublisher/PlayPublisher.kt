package com.github.triplet.gradle.androidpublisher

import com.github.triplet.gradle.androidpublisher.internal.DefaultPlayPublisher
import com.google.common.annotations.VisibleForTesting
import java.io.File

/**
 * Proxy for the AndroidPublisher API. Separate the build side configuration from API dependencies
 * to make testing easier.
 *
 * For the full API docs, see [here](https://developers.google.com/android-publisher/api-ref).
 */
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

    /** Basic factory to create [PlayPublisher] instances. */
    interface Factory {
        /**
         * Creates a new [PlayPublisher].
         *
         * @param credentials the creds to be converted to a GoogleCredential
         * @param email if the creds are of type PKCS, the service account email
         * @param appId the app's package name
         */
        fun create(
                credentials: File,
                email: String?,
                appId: String
        ): PlayPublisher
    }

    companion object {
        private var factory: Factory = DefaultPlayPublisher

        /** Overwrites the default [PlayPublisher.Factory] with [factory]. */
        @VisibleForTesting
        fun setFactory(factory: Factory) {
            Companion.factory = factory
        }

        /** Creates a new [PlayPublisher]. */
        operator fun invoke(
                credentials: File,
                email: String?,
                appId: String
        ): PlayPublisher = factory.create(credentials, email, appId)
    }
}
