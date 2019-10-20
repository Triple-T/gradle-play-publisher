package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.UpdateProductResponse
import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.DeobfuscationFilesUploadResponse
import com.google.api.services.androidpublisher.model.InAppProduct
import com.google.api.services.androidpublisher.model.Track
import java.io.File

internal class DefaultPlayPublisher(
        private val publisher: AndroidPublisher,
        private val appId: String
) : InternalPlayPublisher {
    override fun getTrack(editId: String, track: String): Track {
        return publisher.edits().tracks().get(appId, editId, track).execute()
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

    override fun uploadDeobfuscationFile(
            editId: String,
            mappingFile: File,
            versionCode: Int
    ): DeobfuscationFilesUploadResponse {
        val mapping = FileContent(MIME_TYPE_STREAM, mappingFile)
        return publisher.edits().deobfuscationfiles()
                .upload(appId, editId, versionCode, "proguard", mapping)
                .trackUploadProgress("mapping file", mappingFile)
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

    private fun readProductFile(product: File) = product.inputStream().use {
        JacksonFactory.getDefaultInstance()
                .createJsonParser(it)
                .parse(InAppProduct::class.java)
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
