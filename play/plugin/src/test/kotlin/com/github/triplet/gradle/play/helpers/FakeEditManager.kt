package com.github.triplet.gradle.play.helpers

import com.github.triplet.gradle.androidpublisher.EditManager
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import java.io.File

abstract class FakeEditManager : EditManager {
    fun install() {
        EditManager.setFactory(object : EditManager.Factory {
            override fun create(publisher: PlayPublisher, editId: String) = this@FakeEditManager
        })
    }

    override fun findMaxAppVersionCode(): Long =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun promoteRelease(
            promoteTrackName: String,
            fromTrackName: String?,
            releaseStatus: ReleaseStatus,
            releaseName: String?,
            releaseNotes: Map<String, String?>,
            userFraction: Double?,
            retainableArtifacts: List<Long>?
    ): Unit =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

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
            releaseNotes: Map<String, String?>,
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
            releaseNotes: Map<String, String?>,
            userFraction: Double?,
            retainableArtifacts: List<Long>?
    ): Unit = throw IllegalStateException("Test wasn't expecting this method to be called.")
}
