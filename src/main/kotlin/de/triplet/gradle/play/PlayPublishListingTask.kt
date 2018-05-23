package de.triplet.gradle.play

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.services.androidpublisher.model.AppDetails
import org.gradle.api.tasks.TaskAction
import java.io.File

open class PlayPublishListingTask : PlayPublishTask() {
    lateinit var inputFolder: File

    @TaskAction
    fun publishListing() {
        if (inputFolder.exists()) {
            publish()

            // Matches if locale have the correct naming e.g. en-US for play store
            inputFolder.listFiles(LocaleFileFilter()).forEach { updateListing(it) }

            inputFolder.toAppDetails(extension.errorOnSizeLimit, project.file(RESOURCES_OUTPUT_PATH))?.let {
                edits.details()
                        .update(variant.applicationId, editId, it)
                        .execute()
            }

            edits.commit(variant.applicationId, editId).execute()
        }
    }

    private fun updateListing(dir: File) {
        val locale = dir.name

        val listingDir = File(dir, LISTING_PATH)
        // Check if listing directory exist
        if (listingDir.exists()) {
            try {
                edits.listings()
                        .update(variant.applicationId, editId, locale, listingDir.toListing(extension.errorOnSizeLimit, project.file(RESOURCES_OUTPUT_PATH)))
                        .execute()
            } catch (e: GoogleJsonResponseException) {
                // In case we are using an unsupported locale Google generates an error that
                // is not exactly helpful. In that case we just wrap the original exception in our own.
                if (e.message != null && e.message.toString().contains("unsupportedListingLanguage")) {
                    throw IllegalArgumentException("Unsupported locale $locale", e)
                }

                // Just rethrow everything else.
                throw e
            }

            // By default this will be skipped – can be enabled in the play extension
            if (extension.uploadImages) {
                // Only one ContentFile allowed for featureGraphic
                ImageTypes.values().forEach { content ->
                    uploadImages(content.getImages(listingDir), locale, content.fileName, content.max)
                }
            }
        }
    }

    private fun uploadImages(images: List<AbstractInputStreamContent>?, locale: String, imageType: String, max: Int) {
        if (images != null) {
            // Delete all images in play store
            edits.images()
                    .deleteall(variant.applicationId, editId, locale, imageType)
                    .execute()

            // After that upload the new images
            if (images.size > max) {
                logger.error("Sorry! You can only upload $max graphic(s) as $imageType")
            } else {
                images.forEach {
                    edits.images()
                            .upload(variant.applicationId, editId, locale, imageType, it)
                            .execute()
                }
            }
        }
    }
}

