package com.github.triplet.gradle.play.internal

import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.services.androidpublisher.AndroidPublisherRequest
import org.gradle.internal.logging.progress.ProgressLogger
import kotlin.math.roundToInt

internal fun AndroidPublisherRequest<*>.trackProgress(logger: ProgressLogger) {
    val uploader = mediaHttpUploader ?: return
    uploader.chunkSize = 4 * MediaHttpUploader.MINIMUM_CHUNK_SIZE
    uploader.setProgressListener {
        if (it.uploadState == MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS) {
            logger.progress("Uploading: ${(it.progress * 100).roundToInt()}% complete")
        }
    }
}
