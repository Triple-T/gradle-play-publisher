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

    override fun uploadBundle(
            bundleFile: File,
            mappingFile: File?,
            strategy: ResolutionStrategy,
            versionCode: Long,
            variantName: String,
            isBuildSkippingCommit: Boolean,
            releaseStatus: ReleaseStatus,
            trackName: String,
            retainableArtifacts: List<Long>?,
            releaseName: String?,
            releaseNotes: Map<String, String?>,
            userFraction: Double
    ): Unit = throw IllegalStateException("Test wasn't expecting this method to be called.")
}
