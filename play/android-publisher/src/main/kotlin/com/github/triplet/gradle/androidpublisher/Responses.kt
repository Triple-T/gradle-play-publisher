package com.github.triplet.gradle.androidpublisher

import com.github.triplet.gradle.androidpublisher.internal.has
import com.google.api.client.googleapis.json.GoogleJsonResponseException

/** Response for an app details request. */
data class GppAppDetails internal constructor(
        /** The default language. */
        val defaultLocale: String?,
        /** Developer contact email. */
        val contactEmail: String?,
        /** Developer contact phone. */
        val contactPhone: String?,
        /** Developer contact website. */
        val contactWebsite: String?
)

/** Response for an app listing request. */
data class GppListing internal constructor(
        /** The listing's language. */
        val locale: String,
        /** The app description. */
        val fullDescription: String?,
        /** The app tagline. */
        val shortDescription: String?,
        /** The app title. */
        val title: String?,
        /** The app promo url. */
        val video: String?
)

/** Response for an app graphic request. */
data class GppImage internal constructor(
        /** The image's download URL. */
        val url: String,
        /** The image's SHA256 hash. */
        val sha256: String
)

/** Response for a track release note request. */
data class ReleaseNote internal constructor(
        /** The release note's track. */
        val track: String,
        /** The release note's language. */
        val locale: String,
        /** The release note. */
        val contents: String
)

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

/** Response for a product request. */
data class GppProduct internal constructor(
        /** The product ID. */
        val sku: String,
        /** The response's full JSON payload. */
        val json: String
)

/** Response for a product update request. */
data class UpdateProductResponse internal constructor(
        /** @return true if the product doesn't exist and needs to be created, false otherwise. */
        val needsCreating: Boolean
)
