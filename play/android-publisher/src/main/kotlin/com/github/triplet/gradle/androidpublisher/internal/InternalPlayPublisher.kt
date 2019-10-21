package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.DeobfuscationFilesUploadResponse
import com.google.api.services.androidpublisher.model.Track
import java.io.File
import java.io.IOException

internal interface InternalPlayPublisher : PlayPublisher {
    @Throws(IOException::class)
    fun getTrack(editId: String, track: String): Track

    @Throws(IOException::class)
    fun listTracks(editId: String): List<Track>

    @Throws(IOException::class)
    fun updateTrack(editId: String, track: Track)

    @Throws(IOException::class)
    fun uploadBundle(editId: String, bundleFile: File): Bundle

    fun uploadDeobfuscationFile(
            editId: String,
            mappingFile: File,
            versionCode: Int
    ): DeobfuscationFilesUploadResponse
}
