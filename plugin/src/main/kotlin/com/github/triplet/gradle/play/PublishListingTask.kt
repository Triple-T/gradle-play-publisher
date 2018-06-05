package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.ImageFileFilter
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTING_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.internal.LocaleFileFilter
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.readProcessed
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
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

            defaultLanguage = File(inputFolder, AppDetail.DEFAULT_LANGUAGE.fileName).orNull()
                    ?.readProcessed(AppDetail.DEFAULT_LANGUAGE.maxLength, errorOnSizeLimit)
            contactEmail = File(inputFolder, AppDetail.CONTACT_EMAIL.fileName).orNull()
                    ?.readProcessed(AppDetail.CONTACT_EMAIL.maxLength, errorOnSizeLimit)
            contactPhone = File(inputFolder, AppDetail.CONTACT_PHONE.fileName).orNull()
                    ?.readProcessed(AppDetail.CONTACT_PHONE.maxLength, errorOnSizeLimit)
            contactWebsite = File(inputFolder, AppDetail.CONTACT_WEBSITE.fileName).orNull()
                    ?.readProcessed(AppDetail.CONTACT_WEBSITE.maxLength, errorOnSizeLimit)
        }

        details().update(variant.applicationId, editId, details).execute()
    }

    private fun AndroidPublisher.Edits.updateListing(editId: String, dir: File) {
        val locale = dir.name
        val listingDir = File(dir, LISTING_PATH).orNull() ?: return

        fun uploadListing() {
            val listing = Listing().apply {
                val errorOnSizeLimit = extension.errorOnSizeLimit

                title = File(listingDir, ListingDetail.TITLE.fileName).orNull()
                        ?.readProcessed(ListingDetail.TITLE.maxLength, errorOnSizeLimit)
                shortDescription = File(listingDir, ListingDetail.SHORT_DESCRIPTION.fileName)
                        .orNull()
                        ?.readProcessed(ListingDetail.SHORT_DESCRIPTION.maxLength, errorOnSizeLimit)
                fullDescription = File(listingDir, ListingDetail.FULL_DESCRIPTION.fileName)
                        .orNull()
                        ?.readProcessed(ListingDetail.FULL_DESCRIPTION.maxLength, errorOnSizeLimit)
                video = File(listingDir, ListingDetail.VIDEO.fileName).orNull()
                        ?.readProcessed(ListingDetail.VIDEO.maxLength, errorOnSizeLimit)
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
