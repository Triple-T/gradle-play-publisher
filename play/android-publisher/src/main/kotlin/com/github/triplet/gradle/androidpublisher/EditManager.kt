package com.github.triplet.gradle.androidpublisher

import java.io.File
import java.util.ServiceLoader

/**
 * Orchestrates all edit based operations.
 *
 * For more information on edits, see [here](https://developers.google.com/android-publisher/edits).
 */
interface EditManager {
    /** Retrieves the highest version code available for this app. */
    fun findMaxAppVersionCode(): Long

    /**
     * Promote a release from [fromTrackName] to [promoteTrackName] with the specified update
     * params.
     */
    fun promoteRelease(
            promoteTrackName: String,
            fromTrackName: String?,
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map</* locale= */String, /* text= */String?>?,
            userFraction: Double?,
            retainableArtifacts: List<Long>?
    )

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
            releaseNotes: Map</* locale= */String, /* text= */String?>?,
            userFraction: Double?,
            retainableArtifacts: List<Long>?
    )

    /**
     * Uploads the given [apkFile].
     *
     * Note: since APKs have splits, APK management is a two step process. The APKs must first be
     * uploaded and then published using [publishApk].
     */
    fun uploadApk(
            apkFile: File,
            mappingFile: File?,
            strategy: ResolutionStrategy,
            versionCode: Long,
            variantName: String,
            mainObbRetainable: Int?,
            patchObbRetainable: Int?
    ): Long?

    /** Publishes a set of APKs uploaded with [uploadApk]. */
    fun publishApk(
            versionCodes: List<Long>,
            didPreviousBuildSkipCommit: Boolean,
            trackName: String,
            releaseStatus: ReleaseStatus,
            releaseName: String?,
            releaseNotes: Map</* locale= */String, /* text= */String?>?,
            userFraction: Double?,
            retainableArtifacts: List<Long>?
    )

    /** Basic factory to create [EditManager] instances. */
    interface Factory {
        /** Creates a new [EditManager]. */
        fun create(publisher: PlayPublisher, editId: String): EditManager
    }

    companion object {
        /** Creates a new [EditManager]. */
        operator fun invoke(
                publisher: PlayPublisher,
                editId: String
        ): EditManager = ServiceLoader.load(Factory::class.java).last()
                .create(publisher, editId)
    }
}
