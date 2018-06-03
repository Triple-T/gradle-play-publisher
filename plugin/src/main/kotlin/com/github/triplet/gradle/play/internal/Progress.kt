package com.github.triplet.gradle.play.internal

import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.services.androidpublisher.AndroidPublisherRequest
import org.gradle.internal.logging.progress.ProgressLogger

internal fun AndroidPublisherRequest<*>.initProgressLogger(logger: ProgressLogger) {
    val uploader = mediaHttpUploader ?: return
    uploader.chunkSize = 4 * MediaHttpUploader.MINIMUM_CHUNK_SIZE
    uploader.setProgressListener {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (it.uploadState) {
            MediaHttpUploader.UploadState.INITIATION_COMPLETE -> logger.started()
            MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS ->
                logger.progress("${it.progress * 100}% complete")
            MediaHttpUploader.UploadState.MEDIA_COMPLETE ->
                logger.completed("Upload successful", false)
        }
    }
}
