package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.ImageFileFilter
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTING_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.internal.LocaleFileFilter
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.initProgressLogger
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.readProcessed
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import java.io.File

open class PublishListingTask : PlayPublishTaskBase() {
    lateinit var inputFolder: File

    @TaskAction
    fun publishListing() {
        check(inputFolder.exists()) {
            "No files found, $inputFolder does not exist"
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

            defaultLanguage = File(inputFolder, ListingDetail.DEFAULT_LANGUAGE.fileName).orNull()
                    ?.readProcessed(ListingDetail.DEFAULT_LANGUAGE.maxLength, errorOnSizeLimit)
            contactEmail = File(inputFolder, ListingDetail.CONTACT_EMAIL.fileName).orNull()
                    ?.readProcessed(ListingDetail.CONTACT_EMAIL.maxLength, errorOnSizeLimit)
            contactPhone = File(inputFolder, ListingDetail.CONTACT_PHONE.fileName).orNull()
                    ?.readProcessed(ListingDetail.CONTACT_PHONE.maxLength, errorOnSizeLimit)
            contactWebsite = File(inputFolder, ListingDetail.CONTACT_WEBSITE.fileName).orNull()
                    ?.readProcessed(ListingDetail.CONTACT_WEBSITE.maxLength, errorOnSizeLimit)
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
            val logger = services[ProgressLoggerFactory::class.java]
                    .newOperation(this@PublishListingTask.javaClass)
            logger.start(
                    "Uploading Play Store listing images for variant ${variant.name}", null)

            for (imageType in ImageType.values()) {
                val typeName = imageType.fileName
                val files = File(listingDir, typeName).listFiles(ImageFileFilter)
                        .sorted()
                        .map { FileContent(MIME_TYPE_IMAGE, it) }

                check(files.size <= imageType.maxNum) {
                    "You can only upload ${imageType.maxNum} graphic(s) for the $typeName"
                }

                logger.progress("Processing $imageType", false)
                images().deleteall(variant.applicationId, editId, locale, typeName).execute()
                for (file in files) {
                    images().upload(
                            variant.applicationId, editId, locale, typeName, file).apply {
                        val childLogger = services[ProgressLoggerFactory::class.java]
                                .newOperation(this@PublishListingTask.javaClass, logger)
                        childLogger.description = "Uploading ${file.file.name}"
                        initProgressLogger(childLogger)
                    }.execute()
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
