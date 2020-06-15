package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.DeobfuscationFilesUploadResponse
import com.google.api.services.androidpublisher.model.Image
import com.google.api.services.androidpublisher.model.Listing
import com.google.api.services.androidpublisher.model.Track
import java.io.File
import java.io.IOException

internal interface InternalPlayPublisher : PlayPublisher {
    val appId: String

    fun getAppDetails(editId: String): AppDetails

    fun getListings(editId: String): List<Listing>

    fun getImages(editId: String, locale: String, type: String): List<Image>

    fun updateDetails(editId: String, details: AppDetails)

    fun updateListing(editId: String, locale: String, listing: Listing)

    fun deleteImages(editId: String, locale: String, type: String)

    fun uploadImage(editId: String, locale: String, type: String, image: File)

    fun getTrack(editId: String, track: String): Track

    fun listTracks(editId: String): List<Track>

    fun updateTrack(editId: String, track: Track)

    @Throws(IOException::class)
    fun uploadBundle(editId: String, bundleFile: File): Bundle

    @Throws(IOException::class)
    fun uploadApk(editId: String, apkFile: File): Apk

    fun attachObb(editId: String, type: String, appVersion: Int, obbVersion: Int)

    fun uploadDeobfuscationFile(
            editId: String,
            mappingFile: File,
            versionCode: Int
    ): DeobfuscationFilesUploadResponse
}
