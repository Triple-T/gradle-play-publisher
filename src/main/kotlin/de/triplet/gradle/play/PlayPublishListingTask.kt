package de.triplet.gradle.play

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
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

            val defaultLanguage = File(inputFolder, FILE_NAME_FOR_DEFAULT_LANGUAGE).firstLine()
            if (defaultLanguage.isNotEmpty()) {
                val email = File(inputFolder, FILE_NAME_FOR_CONTACT_EMAIL).firstLine()
                val phone = File(inputFolder, FILE_NAME_FOR_CONTACT_PHONE).firstLine()
                val web = File(inputFolder, FILE_NAME_FOR_CONTACT_WEBSITE).firstLine()

                val details = AppDetails()
                        .setContactEmail(email)
                        .setContactPhone(phone)
                        .setContactWebsite(web)
                        .setDefaultLanguage(defaultLanguage)

                edits.details()
                        .update(variant.applicationId, editId, details)
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
            val listing = Listing()

            val fileTitle = File(listingDir, FILE_NAME_FOR_TITLE)
            val title = fileTitle.readAndTrim(project, MAX_CHARACTER_LENGTH_FOR_TITLE, extension.errorOnSizeLimit)
            if (title.isNotEmpty()) {
                listing.title = title
            }

            val fileShortDescription = File(listingDir, FILE_NAME_FOR_SHORT_DESCRIPTION)
            val shortDescription = fileShortDescription.readAndTrim(project, MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION, extension.errorOnSizeLimit)
            if (shortDescription.isNotEmpty()) {
                listing.shortDescription = shortDescription
            }

            val fileFullDescription = File(listingDir, FILE_NAME_FOR_FULL_DESCRIPTION)
            val fullDescription = fileFullDescription.readAndTrim(project, MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION, extension.errorOnSizeLimit)
            if (fullDescription.isNotEmpty()) {
                listing.fullDescription = fullDescription
            }

            val video = File(listingDir, FILE_NAME_FOR_VIDEO).firstLine()
            if (video.isNotEmpty()) {
                listing.video = video
            }

            try {
                edits.listings()
                        .update(variant.applicationId, editId, locale, listing)
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
                val featureGraphicContent = getImages(listingDir, "$IMAGE_TYPE_FEATURE_GRAPHIC/")
                uploadImages(featureGraphicContent, locale, IMAGE_TYPE_FEATURE_GRAPHIC, 1)

                // Only one ContentFile allowed for iconGraphic
                val iconGraphicContent = getImages(listingDir, "$IMAGE_TYPE_ICON/")
                uploadImages(iconGraphicContent, locale, IMAGE_TYPE_ICON, 1)

                // Only one ContentFile allowed for promoGraphic
                val promoGraphicContent = getImages(listingDir, "$IMAGE_TYPE_PROMO_GRAPHIC/")
                uploadImages(promoGraphicContent, locale, IMAGE_TYPE_PROMO_GRAPHIC, 1)

                // Only one ContentFile allowed for tvBanner
                val tvBannerGraphicContent = getImages(listingDir, "$IMAGE_TYPE_TV_BANNER/")
                uploadImages(tvBannerGraphicContent, locale, IMAGE_TYPE_TV_BANNER, 1)

                // Upload phoneScreenshots
                val phoneContentList = getImages(listingDir, "$IMAGE_TYPE_PHONE_SCREENSHOTS/")
                uploadImages(phoneContentList, locale, IMAGE_TYPE_PHONE_SCREENSHOTS, 1)

                // Upload sevenInchScreenshots
                val sevenInchContentList = getImages(listingDir, "$IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS/")
                uploadImages(sevenInchContentList, locale, IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS)

                // Upload tenInchScreenshots
                val tenInchContentList = getImages(listingDir, "$IMAGE_TYPE_TEN_INCH_SCREENSHOTS/")
                uploadImages(tenInchContentList, locale, IMAGE_TYPE_TEN_INCH_SCREENSHOTS)

                // Upload tvScreenshots
                val tvContentList = getImages(listingDir, "$IMAGE_TYPE_TV_SCREENSHOTS/")
                uploadImages(tvContentList, locale, IMAGE_TYPE_TV_SCREENSHOTS)

                // Upload wearScreenshots
                val wearContentList = getImages(listingDir, "$IMAGE_TYPE_WEAR_SCREENSHOTS/")
                uploadImages(wearContentList, locale, IMAGE_TYPE_WEAR_SCREENSHOTS)
            }
        }
    }

    private fun uploadImages(images: List<AbstractInputStreamContent>?, locale: String, imageType: String, max: Int = MAX_SCREENSHOTS_SIZE) {
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

    private fun getImages(listingDir: File, graphicPath: String): List<AbstractInputStreamContent>? {
        val graphicDir = File(listingDir, graphicPath)
        if (graphicDir.exists()) {
            return graphicDir.listFiles(ImageFileFilter())
                    .asList()
                    .sorted()
                    .map { FileContent(MIME_TYPE_IMAGE, it) }
        }
        return null
    }
}

