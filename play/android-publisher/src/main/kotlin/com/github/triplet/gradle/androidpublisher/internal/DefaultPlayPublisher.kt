package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.UpdateProductResponse
import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.DeobfuscationFilesUploadResponse
import com.google.api.services.androidpublisher.model.InAppProduct
import com.google.api.services.androidpublisher.model.Track
import java.io.File
import kotlin.math.roundToInt

internal class DefaultPlayPublisher(
        private val publisher: AndroidPublisher,
        private val appId: String
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

    override fun commitEdit(id: String) {
        publisher.edits().commit(appId, id).execute()
    }

    override fun getTrack(editId: String, track: String): Track {
        return publisher.edits().tracks().get(appId, editId, track).execute()
    }

    override fun listTracks(editId: String): List<Track> {
        return publisher.edits().tracks().list(appId, editId).execute().tracks.orEmpty()
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

    private fun <T, R : AbstractGoogleClientRequest<T>> R.trackUploadProgress(
            thing: String,
            file: File
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
            }
        }
        return this
    }

    companion object : PlayPublisher.Factory {
        private const val MIME_TYPE_STREAM = "application/octet-stream"
        private const val MIME_TYPE_APK = "application/vnd.android.package-archive"

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
