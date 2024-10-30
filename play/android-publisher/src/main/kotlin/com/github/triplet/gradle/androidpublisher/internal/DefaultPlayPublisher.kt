package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.CommitResponse
import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.GppProduct
import com.github.triplet.gradle.androidpublisher.GppSubscription
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.UpdateProductResponse
import com.github.triplet.gradle.androidpublisher.UpdateSubscriptionResponse
import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.DeobfuscationFilesUploadResponse
import com.google.api.services.androidpublisher.model.ExpansionFile
import com.google.api.services.androidpublisher.model.Image
import com.google.api.services.androidpublisher.model.InAppProduct
import com.google.api.services.androidpublisher.model.Listing
import com.google.api.services.androidpublisher.model.Subscription
import com.google.api.services.androidpublisher.model.Track
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt

internal class DefaultPlayPublisher(
        private val publisher: AndroidPublisher,
        override val appId: String,
) : InternalPlayPublisher {
    override fun insertEdit(): EditResponse {
        return try {
            EditResponse.Success(publisher.edits().insert(appId, null).execute().id)
        } catch (e: GoogleJsonResponseException) {
            EditResponse.Failure(e)
        }
    }

    override fun getEdit(id: String): EditResponse {
        return try {
            EditResponse.Success(publisher.edits().get(appId, id).execute().id)
        } catch (e: GoogleJsonResponseException) {
            EditResponse.Failure(e)
        }
    }

    override fun commitEdit(id: String, sendChangesForReview: Boolean): CommitResponse {
        return try {
            publisher.edits().commit(appId, id)
                    .setChangesNotSentForReview(!sendChangesForReview)
                    .execute()
            CommitResponse.Success
        } catch (e: GoogleJsonResponseException) {
            CommitResponse.Failure(e)
        }
    }

    override fun validateEdit(id: String) {
        publisher.edits().validate(appId, id).execute()
    }

    override fun getAppDetails(editId: String): AppDetails {
        return publisher.edits().details().get(appId, editId).execute()
    }

    override fun getListings(editId: String): List<Listing> {
        return publisher.edits().listings().list(appId, editId).execute()?.listings.orEmpty()
    }

    override fun getImages(editId: String, locale: String, type: String): List<Image> {
        val response = publisher.edits().images().list(appId, editId, locale, type).execute()
        return response?.images.orEmpty()
    }

    override fun updateDetails(editId: String, details: AppDetails) {
        publisher.edits().details().update(appId, editId, details).execute()
    }

    override fun updateListing(editId: String, locale: String, listing: Listing) {
        publisher.edits().listings().update(appId, editId, locale, listing).execute()
    }

    override fun deleteImages(editId: String, locale: String, type: String) {
        publisher.edits().images().deleteall(appId, editId, locale, type).execute()
    }

    override fun uploadImage(editId: String, locale: String, type: String, image: File) {
        val content = FileContent(MIME_TYPE_IMAGE, image)
        publisher.edits().images().upload(appId, editId, locale, type, content).execute()
    }

    override fun getTrack(editId: String, track: String): Track {
        return try {
            publisher.edits().tracks().get(appId, editId, track).execute()
        } catch (e: GoogleJsonResponseException) {
            if (e has "notFound") {
                Track().setTrack(track)
            } else {
                throw e
            }
        }
    }

    override fun listTracks(editId: String): List<Track> {
        return publisher.edits().tracks().list(appId, editId).execute()?.tracks.orEmpty()
    }

    override fun updateTrack(editId: String, track: Track) {
        println("Updating ${track.releases.map { it.status }.distinct()} release " +
                        "($appId:${track.releases.flatMap { it.versionCodes.orEmpty() }}) " +
                        "in track '${track.track}'")
        publisher.edits().tracks().update(appId, editId, track.track, track).execute()
    }

    override fun uploadBundle(editId: String, bundleFile: File): Bundle {
        val content = FileContent(MIME_TYPE_STREAM, bundleFile)
        return publisher.edits().bundles().upload(appId, editId, content)
                .trackUploadProgress("App Bundle", bundleFile)
                .execute()
    }

    override fun uploadApk(editId: String, apkFile: File): Apk {
        val content = FileContent(MIME_TYPE_APK, apkFile)
        return publisher.edits().apks().upload(appId, editId, content)
                .trackUploadProgress("APK", apkFile)
                .execute()
    }

    override fun attachObb(editId: String, type: String, appVersion: Int, obbVersion: Int) {
        val obb = ExpansionFile().also { it.referencesVersion = obbVersion }
        publisher.edits().expansionfiles()
                .update(appId, editId, appVersion, type, obb)
                .execute()
    }

    override fun uploadDeobfuscationFile(
            editId: String,
            file: File,
            versionCode: Int,
            type: String,
    ): DeobfuscationFilesUploadResponse {
        val mapping = FileContent(MIME_TYPE_STREAM, file)
        val humanFileName = when (type) {
            "proguard" -> "mapping"
            "nativeCode" -> "native debug symbols"
            else -> type
        }
        return publisher.edits().deobfuscationfiles()
                .upload(appId, editId, versionCode, type, mapping)
                .trackUploadProgress("$humanFileName file", file)
                .execute()
    }

    override fun uploadInternalSharingBundle(bundleFile: File): UploadInternalSharingArtifactResponse {
        val bundle = publisher.internalappsharingartifacts()
                .uploadbundle(appId, FileContent(MIME_TYPE_STREAM, bundleFile))
                .trackUploadProgress("App Bundle", bundleFile)
                .execute()

        return UploadInternalSharingArtifactResponse(bundle.toPrettyString(), bundle.downloadUrl)
    }

    override fun uploadInternalSharingApk(apkFile: File): UploadInternalSharingArtifactResponse {
        val apk = publisher.internalappsharingartifacts()
                .uploadapk(appId, FileContent(MIME_TYPE_APK, apkFile))
                .trackUploadProgress("APK", apkFile)
                .execute()

        return UploadInternalSharingArtifactResponse(apk.toPrettyString(), apk.downloadUrl)
    }

    override fun getInAppProducts(): List<GppProduct> {
        fun AndroidPublisher.Inappproducts.List.withToken(token: String?) = apply {
            this.token = token
        }

        val products = mutableListOf<InAppProduct>()

        var token: String? = null
        do {
            val response = publisher.inappproducts().list(appId).withToken(token).execute()
            products += response.inappproduct.orEmpty()
            token = response.tokenPagination?.nextPageToken
        } while (token != null)

        return products.map {
            GppProduct(it.sku, it.toPrettyString())
        }
    }

    override fun insertInAppProduct(productFile: File) {
        publisher.inappproducts().insert(appId, readProductFile(productFile))
                .apply { autoConvertMissingPrices = true }
                .execute()
    }

    override fun updateInAppProduct(productFile: File): UpdateProductResponse {
        val product = readProductFile(productFile)
        try {
            publisher.inappproducts().update(appId, product.sku, product)
                    .apply { autoConvertMissingPrices = true }
                    .execute()
        } catch (e: GoogleJsonResponseException) {
            if (e.statusCode == 404) {
                return UpdateProductResponse(true)
            } else {
                throw e
            }
        }

        return UpdateProductResponse(false)
    }

    override fun getInAppSubscriptions(): List<GppSubscription> {
        fun AndroidPublisher.Monetization.Subscriptions.List.withPageToken(pageToken: String?) = apply {
            this.pageToken = pageToken
        }

        val subscriptions = mutableListOf<Subscription>()

        var token: String? = null
        do {
            val response = publisher.monetization().subscriptions().list(appId).withPageToken(token).execute()
            subscriptions += response?.subscriptions.orEmpty()
            token = response?.nextPageToken
        } while (token != null)

        return subscriptions.map {
            GppSubscription(it.productId, it.toPrettyString())
        }
    }

    override fun insertInAppSubscription(subscriptionFile: File, regionsVersion: String) {
        val subscription = readSubscriptionFile(subscriptionFile)
        publisher.monetization().subscriptions().create(subscription.packageName, subscription)
                .apply {
                    regionsVersionVersion = regionsVersion
                    productId = subscription.productId
                }
                .execute()
    }

    override fun updateInAppSubscription(subscriptionFile: File, regionsVersion: String): UpdateSubscriptionResponse {
        val subscription = readSubscriptionFile(subscriptionFile)
        try {
            publisher.monetization().subscriptions().patch(subscription.packageName, subscription.productId, subscription)
                    .apply {
                        regionsVersionVersion = regionsVersion
                        updateMask = SUBSCRIPTIONS_UPDATE_MASK
                    }
                    .execute()
        } catch (e: GoogleJsonResponseException) {
            if (e.statusCode == 404) {
                return UpdateSubscriptionResponse(true)
            } else {
                throw e
            }
        }

        return UpdateSubscriptionResponse(false)
    }

    private fun readProductFile(product: File) = product.inputStream().use {
        GsonFactory.getDefaultInstance()
                .createJsonParser(it)
                .parse(InAppProduct::class.java)
    }

    private fun readSubscriptionFile(product: File) = product.inputStream().use {
        GsonFactory.getDefaultInstance()
                .createJsonParser(it)
                .parse(Subscription::class.java)
    }

    private fun <T, R : AbstractGoogleClientRequest<T>> R.trackUploadProgress(
            thing: String,
            file: File,
    ): R {
        mediaHttpUploader?.setProgressListener {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (it.uploadState) {
                MediaHttpUploader.UploadState.INITIATION_STARTED ->
                    println("Starting $thing upload: $file")
                MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS ->
                    println("Uploading $thing: ${(it.progress * 100).roundToInt()}% complete")
                MediaHttpUploader.UploadState.MEDIA_COMPLETE ->
                    println("${thing.capitalize()} upload complete")
                MediaHttpUploader.UploadState.NOT_STARTED,
                MediaHttpUploader.UploadState.INITIATION_COMPLETE -> {}
            }
        }
        return this
    }

    class Factory : PlayPublisher.Factory {
        override fun create(
                credentials: InputStream,
                appId: String,
        ): PlayPublisher {
            val publisher = createPublisher(credentials)
            return DefaultPlayPublisher(publisher, appId)
        }

        override fun create(appId: String, impersonateServiceAccount: String?): PlayPublisher {
            val publisher = createPublisher(impersonateServiceAccount)
            return DefaultPlayPublisher(publisher, appId)
        }
    }

    private companion object {
        const val MIME_TYPE_STREAM = "application/octet-stream"
        const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        const val MIME_TYPE_IMAGE = "image/*"

        const val SUBSCRIPTIONS_UPDATE_MASK = "listings,basePlans"
    }
}
