package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.InternalSharingArtifact
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.InAppProduct
import java.io.File

internal class DefaultPlayPublisher(
        private val publisher: AndroidPublisher,
        private val appId: String
) : PlayPublisher {
    override fun uploadInternalSharingBundle(bundleFile: File): InternalSharingArtifact {
        val bundle = publisher.internalappsharingartifacts()
                .uploadbundle(appId, FileContent(MIME_TYPE_STREAM, bundleFile))
                .trackUploadProgress("App Bundle", bundleFile)
                .execute()

        return InternalSharingArtifact(bundle.toPrettyString(), bundle.downloadUrl)
    }

    override fun uploadInternalSharingApk(apkFile: File): InternalSharingArtifact {
        val apk = publisher.internalappsharingartifacts()
                .uploadapk(appId, FileContent(MIME_TYPE_APK, apkFile))
                .trackUploadProgress("APK", apkFile)
                .execute()

        return InternalSharingArtifact(apk.toPrettyString(), apk.downloadUrl)
    }

    override fun publishInAppProduct(product: InAppProduct) {
        try {
            publisher.inappproducts().update(appId, product.sku, product)
                    .apply { autoConvertMissingPrices = true }
                    .execute()
        } catch (e: GoogleJsonResponseException) {
            if (e.statusCode == 404) {
                publisher.inappproducts().insert(appId, product)
                        .apply { autoConvertMissingPrices = true }
                        .execute()
            } else {
                throw e
            }
        }
    }

    companion object : PlayPublisher.Factory {
        override fun create(
                credentials: File,
                email: String?,
                appId: String
        ): PlayPublisher {
            val publisher = createPublisher(ServiceAccountAuth(credentials, email))
            return DefaultPlayPublisher(publisher, appId)
        }
    }
}
