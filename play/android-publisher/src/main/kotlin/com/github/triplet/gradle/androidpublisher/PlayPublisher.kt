package com.github.triplet.gradle.androidpublisher

import java.io.File
import java.io.InputStream
import java.util.ServiceLoader

/**
 * Proxy for the AndroidPublisher API. Separate the build side configuration from API dependencies
 * to make testing easier.
 *
 * For the full API docs, see [here](https://developers.google.com/android-publisher/api-ref).
 */
interface PlayPublisher {
    /**
     * Creates a new edit.
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/edits/insert).
     */
    fun insertEdit(): EditResponse

    /**
     * Retrieves an existing edit with the given [id].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/edits/get).
     */
    fun getEdit(id: String): EditResponse

    /**
     * Commits an edit with the given [id].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/edits/commit).
     */
    fun commitEdit(id: String, sendChangesForReview: Boolean = true): CommitResponse

    /**
     * Validates an edit with the given [id].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/edits/validate).
     */
    fun validateEdit(id: String)

    /**
     * Uploads the given [bundleFile] as an Internal Sharing artifact.
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/internalappsharingartifacts/uploadbundle).
     */
    fun uploadInternalSharingBundle(bundleFile: File): UploadInternalSharingArtifactResponse

    /**
     * Uploads the given [apkFile] as an Internal Sharing artifact.
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/internalappsharingartifacts/uploadapk).
     */
    fun uploadInternalSharingApk(apkFile: File): UploadInternalSharingArtifactResponse

    /**
     * Get all current products.
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/inappproducts/list).
     */
    fun getInAppProducts(): List<GppProduct>

    /**
     * Creates a new product from the given [productFile].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/inappproducts/insert).
     */
    fun insertInAppProduct(productFile: File)

    /**
     * Updates an existing product from the given [productFile].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/inappproducts/update).
     */
    fun updateInAppProduct(productFile: File): UpdateProductResponse

    /**
     * Get all current subscriptions.
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions/list).
     */
    fun getInAppSubscriptions(): List<GppSubscription>

    /**
     * Creates a new subscription from the given [subscriptionFile].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions/create).
     */
    fun insertInAppSubscription(subscriptionFile: File, regionsVersion: String)

    /**
     * Updates an existing subscription from the given [subscriptionFile].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions/patch).
     */
    fun updateInAppSubscription(subscriptionFile: File, regionsVersion: String): UpdateSubscriptionResponse

    /** Basic factory to create [PlayPublisher] instances. */
    interface Factory {
        /**
         * Creates a new [PlayPublisher].
         *
         * @param credentials the creds to be converted to a GoogleCredential
         * @param appId the app's package name
         */
        fun create(
                credentials: InputStream,
                appId: String,
        ): PlayPublisher

        /**
         * Creates a new [PlayPublisher] using ApplicationDefaultCredentials.
         *
         * @param appId the app's package name
         */
        fun create(
                appId: String,
                impersonateServiceAccount: String?
        ): PlayPublisher
    }

    companion object {
        /** The suggested environment variable name in which to store credentials. */
        const val CREDENTIAL_ENV_VAR: String = "ANDROID_PUBLISHER_CREDENTIALS"

        /** Creates a new [PlayPublisher]. */
        operator fun invoke(
                credentials: InputStream,
                appId: String,
        ): PlayPublisher = ServiceLoader.load(Factory::class.java).last()
                .create(credentials, appId)

        /** Creates a new [PlayPublisher] using Application Default Credentials on GCP
         *  and using Service Account Impersonation if a Service Account to impersonate is
         *  configured.
         * */
        operator fun invoke(
                appId: String,
                impersonateServiceAccount: String?
        ): PlayPublisher = ServiceLoader.load(Factory::class.java).last()
                .create(appId, impersonateServiceAccount)
    }
}
