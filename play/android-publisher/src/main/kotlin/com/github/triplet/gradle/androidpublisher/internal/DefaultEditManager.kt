package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.EditManager
import com.github.triplet.gradle.androidpublisher.GppAppDetails
import com.github.triplet.gradle.androidpublisher.GppImage
import com.github.triplet.gradle.androidpublisher.GppListing
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseNote
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
import org.slf4j.LoggerFactory
import java.io.File

internal class DefaultEditManager(
        private val publisher: InternalPlayPublisher,
        private val tracks: TrackManager,
        private val editId: String
) : EditManager {
    override fun getAppDetails(): GppAppDetails {
        val details = publisher.getAppDetails(editId)
        return GppAppDetails(
                details.defaultLanguage,
                details.contactEmail,
                details.contactPhone,
                details.contactWebsite
        )
    }

    override fun getListings(): List<GppListing> {
        return publisher.getListings(editId).map {
            GppListing(
                    it.language,
                    it.fullDescription,
                    it.shortDescription,
                    it.title,
                    it.video
            )
        }
    }

    override fun getImages(locale: String, type: String): List<GppImage> {
        return publisher.getImages(editId, locale, type).map {
            GppImage(it.url + HIGH_RES_IMAGE_REQUEST, it.sha256)
        }
    }

    override fun findMaxAppVersionCode(): Long {
        return tracks.findHighestTrack()?.releases.orEmpty()
                .flatMap { it.versionCodes.orEmpty() }
                .max() ?: 1
    }

    override fun findLeastStableTrackName(): String? {
        return tracks.findHighestTrack()?.track
    }

    override fun getReleaseNotes(): List<ReleaseNote> {
        return tracks.getReleaseNotes().map { (track, notes) ->
            notes.map { ReleaseNote(track, it.language, it.text) }
        }.flatten()
    }

    override fun publishAppDetails(
            defaultLocale: String?,
            contactEmail: String?,
            contactPhone: String?,
            contactWebsite: String?
    ) {
        publisher.updateDetails(editId, AppDetails().apply {
            this.defaultLanguage = defaultLocale
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
            fromTrackName: String,
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
            updatePriority: Int?,
            retainableArtifacts: List<Long>?
    ) {
        tracks.promote(TrackManager.PromoteConfig(
                promoteTrackName,
                fromTrackName,
                TrackManager.BaseConfig(
                        releaseStatus,
                        userFraction,
                        updatePriority,
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
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
            updatePriority: Int?,
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
                didPreviousBuildSkipCommit,
                TrackManager.BaseConfig(
                        releaseStatus,
                        userFraction,
                        updatePriority,
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
            releaseStatus: ReleaseStatus?,
            releaseName: String?,
            releaseNotes: Map<String, String?>?,
            userFraction: Double?,
            updatePriority: Int?,
            retainableArtifacts: List<Long>?
    ) {
        if (versionCodes.isEmpty()) return

        tracks.update(TrackManager.UpdateConfig(
                trackName,
                versionCodes,
                didPreviousBuildSkipCommit,
                TrackManager.BaseConfig(
                        releaseStatus,
                        userFraction,
                        updatePriority,
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
            ResolutionStrategy.IGNORE -> LoggerFactory.getLogger(EditManager::class.java).warn(
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

    private companion object {
        const val HIGH_RES_IMAGE_REQUEST = "=h16383" // Max res: 2^14 - 1
    }
}
