package com.github.triplet.gradle.androidpublisher

import com.github.triplet.gradle.androidpublisher.internal.has
import com.google.api.client.googleapis.json.GoogleJsonResponseException

/** Response for an edit request. */
sealed class EditResponse {
    /** Response for a successful edit request. */
    data class Success internal constructor(
            /** The id of the edit in question. */
            val id: String
    ) : EditResponse()

    /** Response for an unsuccessful edit request. */
    class Failure internal constructor(
            private val e: GoogleJsonResponseException
    ) : EditResponse() {
        /** @return true if the app wasn't found in the Play Console, false otherwise */
        fun isNewApp(): Boolean = e has "applicationNotFound"

        /** @return true if the provided edit is invalid for any reason, false otherwise */
        fun isInvalidEdit(): Boolean =
                e has "editAlreadyCommitted" || e has "editNotFound" || e has "editExpired"

        /** @return true if the user doesn't have permission to access this app, false otherwise */
        fun isUnauthorized(): Boolean = e.statusCode == 401

        /** Cleanly rethrows the error. */
        fun rethrow(): Nothing = throw e

        /** Wraps the error in a new exception with the provided [newMessage]. */
        fun rethrow(newMessage: String): Nothing = throw IllegalStateException(newMessage, e)
    }
}

/** Response for an internal sharing artifact upload. */
data class UploadInternalSharingArtifactResponse internal constructor(
        /** The response's full JSON payload. */
        val json: String,

        /** The download URL of the uploaded artifact. */
        val downloadUrl: String
)

/** Response for a product update request. */
data class UpdateProductResponse internal constructor(
        /** @return true if the product doesn't exist and needs to be created, false otherwise. */
        val needsCreating: Boolean
)
