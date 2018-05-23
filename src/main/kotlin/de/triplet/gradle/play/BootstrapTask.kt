package de.triplet.gradle.play

import org.gradle.api.tasks.TaskAction
import java.io.File

open class BootstrapTask : PlayPublishTask() {
    lateinit var outputFolder: File

    @TaskAction
    fun bootstrap() {
        publish()

        bootstrapListing()
        bootstrapWhatsNew()
        bootstrapAppDetails()
    }

    private fun bootstrapListing() {
        val listings = edits.listings()
                .list(variant.applicationId, editId)
                .execute()
                .listings ?: return

        listings.forEach { listing ->
            val listingDir = outputFolder.validSubFolder(listing.language, LISTING_PATH) ?: return@forEach

            ImageTypes.values().forEach { imageType ->
                val images = edits.images()
                        .list(variant.applicationId, editId, listing.language, imageType.fileName)
                        .execute()
                        .images
                imageType.saveImages(listingDir, images)
            }

            listing.saveText(listingDir)
        }
    }

    private fun bootstrapWhatsNew() {
        val apks = edits.apks()
                .list(variant.applicationId, editId)
                .execute()
                .apks ?: return
        val versionCode = apks.map { it.versionCode }.max()

        val apkListings = edits.apklistings()
                .list(variant.applicationId, editId, versionCode)
                .execute()
                .listings ?: return

        apkListings.forEach { apkListing ->
            outputFolder.validSubFolder(apkListing.language)?.let { languageDir ->
                File(languageDir, ListingDetails.WhatsNew.fileName).writeText(apkListing.recentChanges)
            }
        }
    }

    private fun bootstrapAppDetails() {
        edits.details()
                .get(variant.applicationId, editId)
                .execute().saveText(outputFolder)
    }
}
