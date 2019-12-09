package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.EditManager
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
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

    override fun fetchImageHashes(locale: String, type: String): List<String> {
        return publisher.getImages(editId, locale, type).map { it.sha256 }
    }

    override fun publishAppDetails(
            defaultLanguage: String?,
            contactEmail: String?,
            contactPhone: String?,
            contactWebsite: String?
    ) {
        publisher.updateDetails(editId, AppDetails().apply {
            this.defaultLanguage = defaultLanguage
            this.contactEmail = contactEmail
            this.contactPhone = contactPhone
            this.contactWebsite = contactWebsite
        })
    }

    override fun publishListing(
            locale: String,
            title: String?,
            shortDescription: String?,
            fullDescription: String?,
            video: String?
    ) {
        publisher.updateListing(editId, locale, Listing().apply {
            this.title = title
            this.shortDescription = shortDescription
            this.fullDescription = fullDescription
            this.video = video
        })
    }

    override fun publishImages(locale: String, type: String, images: List<File>) {
        publisher.deleteImages(editId, locale, type)
        for (image in images) {
            println("Uploading $locale listing graphic for type '$type': ${image.name}")
            // These can't be uploaded in parallel because order matters
            publisher.uploadImage(editId, locale, type, image)
        }
    }

    override fun promoteRelease(
            promoteTrackName: String,
            fromTrackName: String?,
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
            retainableArtifacts: List<Long>?
    ) {
        tracks.promote(TrackManager.PromoteConfig(
                promoteTrackName,
                fromTrackName,
                TrackManager.BaseConfig(
                        releaseStatus,
                        userFraction,
                        releaseNotes,
                        retainableArtifacts,
                        releaseName
                )
        ))
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
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
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
                didPreviousBuildSkipCommit,
                TrackManager.BaseConfig(
                        releaseStatus,
                        userFraction,
                        releaseNotes,
                        retainableArtifacts,
                        releaseName
                )
        ))
    }

    override fun uploadApk(
            apkFile: File,
            mappingFile: File?,
            strategy: ResolutionStrategy,
            versionCode: Long,
            variantName: String,
            mainObbRetainable: Int?,
            patchObbRetainable: Int?
    ): Long? {
        val apk = try {
            publisher.uploadApk(editId, apkFile)
        } catch (e: GoogleJsonResponseException) {
            handleUploadFailures(e, strategy, apkFile, versionCode, variantName)
            return null
        }

        mainObbRetainable?.attachObb("main", apk.versionCode)
        patchObbRetainable?.attachObb("patch", apk.versionCode)

        uploadMappingFile(apk.versionCode, mappingFile)

        return apk.versionCode.toLong()
    }

    override fun publishApk(
            versionCodes: List<Long>,
            didPreviousBuildSkipCommit: Boolean,
            trackName: String,
            releaseStatus: ReleaseStatus,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
            retainableArtifacts: List<Long>?
    ) {
        if (versionCodes.isEmpty()) return

        tracks.update(TrackManager.UpdateConfig(
                trackName,
                versionCodes,
                releaseStatus,
                didPreviousBuildSkipCommit,
                TrackManager.BaseConfig(
                        releaseStatus,
                        userFraction,
                        releaseNotes,
                        retainableArtifacts,
                        releaseName
                )
        ))
    }

    private fun uploadMappingFile(versionCode: Int, mappingFile: File?) {
        if (mappingFile != null && mappingFile.length() > 0) {
            publisher.uploadDeobfuscationFile(editId, mappingFile, versionCode)
        }
    }

    private fun Int.attachObb(type: String, versionCode: Int) {
        println("Attaching $type OBB ($this) to APK $versionCode")
        publisher.attachObb(editId, type, versionCode, this)
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

    class Factory : EditManager.Factory {
        override fun create(publisher: PlayPublisher, editId: String) = DefaultEditManager(
                publisher as InternalPlayPublisher,
                DefaultTrackManager(publisher, editId),
                editId
        )
    }
}
