package com.github.triplet.gradle.androidpublisher

import java.io.File

abstract class FakeEditManager : EditManager {
    fun install() {
        manager = this
    }

    override fun findMaxAppVersionCode(): Long =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun promoteRelease(
            promoteTrackName: String,
            fromTrackName: String?,
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
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
            releaseStatus: ReleaseStatus,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
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
            releaseStatus: ReleaseStatus,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
            retainableArtifacts: List<Long>?
    ): Unit = throw IllegalStateException("Test wasn't expecting this method to be called.")

    class Factory : EditManager.Factory {
        override fun create(publisher: PlayPublisher, editId: String) = manager
    }

    companion object {
        lateinit var manager: EditManager
    }
}
