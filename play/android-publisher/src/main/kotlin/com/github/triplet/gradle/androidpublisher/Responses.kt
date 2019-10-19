package com.github.triplet.gradle.androidpublisher

/** Response for an internal sharing artifact upload. */
data class InternalSharingArtifact(
        /** The response's full JSON payload. */
        val json: String,

        /** The download URL of the uploaded artifact. */
        val downloadUrl: String
)
