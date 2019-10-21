package com.github.triplet.gradle.androidpublisher

import com.github.triplet.gradle.androidpublisher.internal.DefaultEditManager
import com.google.common.annotations.VisibleForTesting
import java.io.File

/**
 * Orchestrates all edit based operations.
 *
 * For more information on edits, see [here](https://developers.google.com/android-publisher/edits).
 */
interface EditManager {
    /** Retrieves the highest version code available for this app. */
    fun findMaxAppVersionCode(): Long

    /** Uploads and publishes the given [bundleFile]. */
    fun uploadBundle(
            bundleFile: File,
            mappingFile: File?,
            strategy: ResolutionStrategy,
            versionCode: Long,
            variantName: String,
            didPreviousBuildSkipCommit: Boolean,
            trackName: String,
            releaseStatus: ReleaseStatus,
            releaseName: String?,
            releaseNotes: Map</* locale= */String, /* text= */String?>,
            userFraction: Double,
            retainableArtifacts: List<Long>?
    )

    /** Basic factory to create [EditManager] instances. */
    interface Factory {
        /** Creates a new [EditManager]. */
        fun create(publisher: PlayPublisher, editId: String): EditManager
    }

    companion object {
        private var factory: Factory = DefaultEditManager

        /** Overwrites the default [EditManager.Factory] with [factory]. */
        @VisibleForTesting
        fun setFactory(factory: Factory) {
            Companion.factory = factory
        }

        /** Creates a new [EditManager]. */
        operator fun invoke(
                publisher: PlayPublisher,
                editId: String
        ): EditManager = factory.create(publisher, editId)
    }
}
