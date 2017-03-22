package de.triplet.gradle.play

import com.google.api.services.androidpublisher.model.Image
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

        for (listing in listings) {
            val languageDir = File(outputFolder, listing.language)
            if (!languageDir.exists() && !languageDir.mkdirs()) {
                continue
            }

            val listingDir = File(languageDir, LISTING_PATH)
            if (!listingDir.exists() && !listingDir.mkdirs()) {
                continue
            }

            for (imageType in IMAGE_TYPES) {
                val images = edits.images()
                        .list(variant.applicationId, editId, listing.language, imageType)
                        .execute()
                        .images
                saveImage(listingDir, imageType, images)
            }

            File(listingDir, FILE_NAME_FOR_FULL_DESCRIPTION).writeText(listing.fullDescription)
            File(listingDir, FILE_NAME_FOR_SHORT_DESCRIPTION).writeText(listing.shortDescription)
            File(listingDir, FILE_NAME_FOR_TITLE).writeText(listing.title)
            File(listingDir, FILE_NAME_FOR_VIDEO).writeText(listing.video)
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

        for (apkListing in apkListings) {
            val languageDir = File(outputFolder, apkListing.language)
            if (!languageDir.exists() && !languageDir.mkdirs()) {
                continue
            }

            File(languageDir, FILE_NAME_FOR_WHATS_NEW_TEXT).writeText(apkListing.recentChanges)
        }
    }

    private fun bootstrapAppDetails() {
        val appDetails = edits.details()
                .get(variant.applicationId, editId)
                .execute()

        File(outputFolder, FILE_NAME_FOR_CONTACT_EMAIL).writeText(appDetails.contactEmail)
        File(outputFolder, FILE_NAME_FOR_CONTACT_PHONE).writeText(appDetails.contactPhone)
        File(outputFolder, FILE_NAME_FOR_CONTACT_WEBSITE).writeText(appDetails.contactWebsite)
        File(outputFolder, FILE_NAME_FOR_DEFAULT_LANGUAGE).writeText(appDetails.defaultLanguage)
    }

    private fun saveImage(listingDir: File, imageFolderName: String, images: List<Image>?) {
        val imageFolder = File(listingDir, imageFolderName)
        if (!imageFolder.exists() && !imageFolder.mkdirs()) {
            return
        }

        if (images == null) {
            return
        }

        // TODO: Disabled for now as we have only access to preview-versions with the current API.
        /*
        for (image in images) {
            File(imageFolder, "${image.id}.png").outputStream().use { os ->
                URL(image.url).openStream().use { it.copyTo(os) }
            }
        }
        */
    }
}
