package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.EditManager
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import org.gradle.api.logging.Logging
import java.io.File

internal class DefaultEditManager(
        private val publisher: InternalPlayPublisher,
        private val tracks: TrackManager,
        private val editId: String
) : EditManager {
    override fun findMaxAppVersionCode(): Long {
        return tracks.findMaxAppVersionCode()
    }

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
            userFraction: Double,
            retainableArtifacts: List<Long>?
    ) {
        val bundle = try {
            publisher.uploadBundle(editId, bundleFile)
        } catch (e: GoogleJsonResponseException) {
            handleUploadFailures(e, strategy, bundleFile, versionCode, variantName)
        } ?: return

        uploadMappingFile(bundle.versionCode, mappingFile)
        tracks.update(TrackManager.UpdateConfig(
                trackName,
                listOf(bundle.versionCode.toLong()),
                releaseStatus,
                userFraction,
                releaseNotes,
                retainableArtifacts,
                releaseName,
                didPreviousBuildSkipCommit
        ))
    }

    private fun uploadMappingFile(versionCode: Int, mappingFile: File?) {
        if (mappingFile != null && mappingFile.length() > 0) {
            publisher.uploadDeobfuscationFile(editId, mappingFile, versionCode)
        }
    }

    private fun handleUploadFailures(
            e: GoogleJsonResponseException,
            strategy: ResolutionStrategy,
            artifact: File,
            versionCode: Long,
            variantName: String
    ): Nothing? = if (
            e has "apkNotificationMessageKeyUpgradeVersionConflict" ||
            e has "apkUpgradeVersionConflict" ||
            e has "apkNoUpgradePath"
    ) {
        when (strategy) {
            ResolutionStrategy.AUTO -> throw IllegalStateException(
                    "Concurrent uploads for variant $variantName (version code $versionCode " +
                            "already used). Make sure to synchronously upload your APKs such " +
                            "that they don't conflict. If this problem persists, delete your " +
                            "drafts in the Play Console's artifact library.",
                    e
            )
            ResolutionStrategy.FAIL -> throw IllegalStateException(
                    "Version code $versionCode is too low or has already been used for variant " +
                            "$variantName.",
                    e
            )
            ResolutionStrategy.IGNORE -> Logging.getLogger(EditManager::class.java).warn(
                    "Ignoring artifact ($artifact) for version code $versionCode")
        }
        null
    } else {
        throw e
    }

    companion object : EditManager.Factory {
        override fun create(publisher: PlayPublisher, editId: String) = DefaultEditManager(
                publisher as InternalPlayPublisher,
                DefaultTrackManager(publisher, editId),
                editId
        )
    }
}
