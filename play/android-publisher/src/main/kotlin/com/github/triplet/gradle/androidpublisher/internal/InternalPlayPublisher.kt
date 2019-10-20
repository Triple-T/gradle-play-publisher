package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.DeobfuscationFilesUploadResponse
import com.google.api.services.androidpublisher.model.Track
import java.io.File

internal interface InternalPlayPublisher : PlayPublisher {
    fun getTrack(editId: String, track: String): Track

    fun updateTrack(editId: String, track: Track)

    fun uploadBundle(editId: String, bundleFile: File): Bundle

    fun uploadDeobfuscationFile(
            editId: String,
            mappingFile: File,
            versionCode: Int
    ): DeobfuscationFilesUploadResponse
}
