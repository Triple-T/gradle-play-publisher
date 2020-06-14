package com.github.triplet.gradle.androidpublisher

import java.io.File

abstract class FakeEditManager : EditManager {
    fun install() {
        manager = this
    }

    override fun getAppDetails(): GppAppDetails =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun getListings(): List<GppListing> =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun getImages(locale: String, type: String): List<GppImage> =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun findMaxAppVersionCode(): Long =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun findLeastStableTrackName(): String? =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun getReleaseNotes(): List<ReleaseNote> =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun publishAppDetails(
            defaultLocale: String?,
            contactEmail: String?,
            contactPhone: String?,
            contactWebsite: String?
    ): Unit = throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun publishListing(
            locale: String,
            title: String?,
            shortDescription: String?,
            fullDescription: String?,
            video: String?
    ): Unit = throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun publishImages(locale: String, type: String, images: List<File>): Unit =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun promoteRelease(
            promoteTrackName: String,
            fromTrackName: String,
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
            updatePriority: Int?,
            retainableArtifacts: List<Long>?
    ): Unit = throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun uploadBundle(
            bundleFile: File,
            mappingFile: File?,
            strategy: ResolutionStrategy,
            versionCode: Long,
            variantName: String,
            didPreviousBuildSkipCommit: Boolean,
            trackName: String,
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
            updatePriority: Int?,
            retainableArtifacts: List<Long>?
    ): Unit = throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun uploadApk(
            apkFile: File,
            mappingFile: File?,
            strategy: ResolutionStrategy,
            versionCode: Long,
            variantName: String,
            mainObbRetainable: Int?,
            patchObbRetainable: Int?
    ): Long? = throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun publishApk(
            versionCodes: List<Long>,
            didPreviousBuildSkipCommit: Boolean,
            trackName: String,
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
            updatePriority: Int?,
            retainableArtifacts: List<Long>?
    ): Unit = throw IllegalStateException("Test wasn't expecting this method to be called.")

    class Factory : EditManager.Factory {
        override fun create(publisher: PlayPublisher, editId: String) = manager
    }

    companion object {
        lateinit var manager: EditManager
    }
}
