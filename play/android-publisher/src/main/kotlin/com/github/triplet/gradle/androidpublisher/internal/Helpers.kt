package com.github.triplet.gradle.androidpublisher.internal

import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest
import java.io.File
import kotlin.math.roundToInt

internal const val MIME_TYPE_STREAM = "application/octet-stream"

internal fun <T, R : AbstractGoogleClientRequest<T>> R.trackUploadProgress(
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
