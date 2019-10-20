package com.github.triplet.gradle.androidpublisher

/** Response for an internal sharing artifact upload. */
data class UploadInternalSharingArtifactResponse(
        /** The response's full JSON payload. */
        val json: String,

        /** The download URL of the uploaded artifact. */
        val downloadUrl: String
)

/** Response for a product update request. */
data class UpdateProductResponse(
        /** @return true if the product doesn't exist and needs to be created, false otherwise. */
        val needsCreating: Boolean
)
