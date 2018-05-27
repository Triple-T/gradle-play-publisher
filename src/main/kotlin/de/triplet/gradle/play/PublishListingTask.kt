package de.triplet.gradle.play

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
import de.triplet.gradle.play.internal.ImageFileFilter
import de.triplet.gradle.play.internal.ImageType
import de.triplet.gradle.play.internal.LISTING_PATH
import de.triplet.gradle.play.internal.ListingDetail
import de.triplet.gradle.play.internal.LocaleFileFilter
import de.triplet.gradle.play.internal.PlayPublishTaskBase
import de.triplet.gradle.play.internal.orNull
import de.triplet.gradle.play.internal.readProcessed
import org.gradle.api.tasks.TaskAction
import java.io.File

open class PublishListingTask : PlayPublishTaskBase() {
    lateinit var inputFolder: File

    @TaskAction
    fun publishListing() {
        if (!inputFolder.exists()) {
            logger.info("Skipping listing upload: $inputFolder does not exist.")
            return
        }

        write { editId ->
            updateListings(editId)
            updateAppDetails(editId)
        }
    }

    private fun AndroidPublisher.Edits.updateListings(editId: String) {
        // Matches valid locales e.g. en-US for Play Store
        inputFolder.listFiles(LocaleFileFilter).forEach { updateListing(editId, it) }
    }

    private fun AndroidPublisher.Edits.updateAppDetails(editId: String) {
        val details = AppDetails().apply {
            val errorOnSizeLimit = extension.errorOnSizeLimit

            defaultLanguage = (File(inputFolder, ListingDetail.DEFAULT_LANGUAGE.fileName)
                    .orNull() ?: return)
                    .readProcessed(ListingDetail.DEFAULT_LANGUAGE.maxLength, errorOnSizeLimit)
            contactEmail = (File(inputFolder, ListingDetail.CONTACT_EMAIL.fileName)
                    .orNull() ?: return)
                    .readProcessed(ListingDetail.CONTACT_EMAIL.maxLength, errorOnSizeLimit)
            contactPhone = (File(inputFolder, ListingDetail.CONTACT_PHONE.fileName)
                    .orNull() ?: return)
                    .readProcessed(ListingDetail.CONTACT_PHONE.maxLength, errorOnSizeLimit)
            contactWebsite = (File(inputFolder, ListingDetail.CONTACT_WEBSITE.fileName)
                    .orNull() ?: return)
                    .readProcessed(ListingDetail.CONTACT_WEBSITE.maxLength, errorOnSizeLimit)
        }

        details().update(variant.applicationId, editId, details).execute()
    }

    private fun AndroidPublisher.Edits.updateListing(editId: String, dir: File) {
        val locale = dir.name
        val listingDir = File(dir, LISTING_PATH).orNull() ?: return

        fun uploadListing() {
            val listing = Listing().apply {
                val errorOnSizeLimit = extension.errorOnSizeLimit

                title = (File(inputFolder, ListingDetail.TITLE.fileName)
                        .orNull() ?: return)
                        .readProcessed(ListingDetail.TITLE.maxLength, errorOnSizeLimit)
                shortDescription = (File(inputFolder, ListingDetail.SHORT_DESCRIPTION.fileName)
                        .orNull() ?: return)
                        .readProcessed(ListingDetail.SHORT_DESCRIPTION.maxLength, errorOnSizeLimit)
                fullDescription = (File(inputFolder, ListingDetail.FULL_DESCRIPTION.fileName)
                        .orNull() ?: return)
                        .readProcessed(ListingDetail.FULL_DESCRIPTION.maxLength, errorOnSizeLimit)
                video = (File(inputFolder, ListingDetail.VIDEO.fileName)
                        .orNull() ?: return)
                        .readProcessed(ListingDetail.VIDEO.maxLength, errorOnSizeLimit)
            }

            try {
                listings().update(variant.applicationId, editId, locale, listing).execute()
            } catch (e: GoogleJsonResponseException) {
                if (e.details.errors.any { it.reason == "unsupportedListingLanguage" }) {
                    // Rethrow for clarity
                    throw IllegalArgumentException("Unsupported locale $locale", e)
                } else {
                    throw e
                }
            }
        }

        fun updateImages() {
            for (imageType in ImageType.values()) {
                val typeName = imageType.fileName
                val files = File(listingDir, typeName).listFiles(ImageFileFilter)
                        .sorted()
                        .map { FileContent(MIME_TYPE_IMAGE, it) }

                if (files.isEmpty()) return

                images().deleteall(variant.applicationId, editId, locale, typeName).execute()
                if (files.size <= imageType.maxNum) {
                    for (file in files) {
                        images()
                                .upload(variant.applicationId, editId, locale, typeName, file)
                                .execute()
                    }
                } else {
                    logger.error(
                            "You can only upload ${imageType.maxNum} graphic(s) for the $typeName")
                }
            }
        }

        uploadListing()
        if (extension.uploadImages) updateImages()
    }

    private companion object {
        const val MIME_TYPE_IMAGE = "image/*"
    }
}
