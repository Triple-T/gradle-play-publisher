package com.github.triplet.gradle.androidpublisher

import java.io.File
import java.util.ServiceLoader

/**
 * Orchestrates all edit based operations.
 *
 * For more information on edits, see [here](https://developers.google.com/android-publisher/edits).
 */
interface EditManager {
    /** Retrieves the current app details. */
    fun getAppDetails(): GppAppDetails

    /** Retrieves the current app listings for all languages. */
    fun getListings(): List<GppListing>

    /** Retrieves the app's graphics for the given [locale] and [type]. */
    fun getImages(locale: String, type: String): List<GppImage>

    /** Retrieves the highest version code available for this app. */
    fun findMaxAppVersionCode(): Long

    /** Retrieves the track with the highest version code available for this app. */
    fun findLeastStableTrackName(): String?

    /** Retrieves the release notes across all tracks for this app. */
    fun getReleaseNotes(): List<ReleaseNote>

    /** Publish app details, overwriting any existing values. */
    fun publishAppDetails(
            defaultLocale: String?,
            contactEmail: String?,
            contactPhone: String?,
            contactWebsite: String?
    )

    /**
     * Publish an app listing for the given [locale], overwriting any existing values.
     *
     * Note: valid locales may be found
     * [here](https://support.google.com/googleplay/android-developer/table/4419860?hl=en).
     */
    fun publishListing(
            locale: String,
            title: String?,
            shortDescription: String?,
            fullDescription: String?,
            video: String?
    )

    /** Publish images for a given [locale] and [type], overwriting any existing values. */
    fun publishImages(locale: String, type: String, images: List<File>)

    /**
     * Promote a release from [fromTrackName] to [promoteTrackName] with the specified update
     * params.
     */
    fun promoteRelease(
            promoteTrackName: String,
            fromTrackName: String,
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map</* locale= */String, /* text= */String?>?,
            userFraction: Double?,
            updatePriority: Int?,
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
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map</* locale= */String, /* text= */String?>?,
            userFraction: Double?,
            updatePriority: Int?,
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
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map</* locale= */String, /* text= */String?>?,
            userFraction: Double?,
            updatePriority: Int?,
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
