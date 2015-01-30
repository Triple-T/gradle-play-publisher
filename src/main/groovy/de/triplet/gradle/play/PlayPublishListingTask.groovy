package de.triplet.gradle.play

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Listing
import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.TaskAction

class PlayPublishListingTask extends PlayPublishTask {

    static def MAX_CHARACTER_LENGTH_FOR_TITLE = 30
    static def MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION = 80
    static def MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION = 4000
    static def MAX_SCREENSHOTS_SIZE = 8

    static def FILE_NAME_FOR_TITLE = "title"
    static def FILE_NAME_FOR_SHORT_DESCRIPTION = "shortdescription"
    static def FILE_NAME_FOR_FULL_DESCRIPTION = "fulldescription"
    static def LISTING_PATH = "listing/"

    static def IMAGE_TYPE_FEATURE_GRAPHIC = "featureGraphic"
    static def IMAGE_TYPE_ICON = "icon"
    static def IMAGE_TYPE_PHONE_SCREENSHOTS = "phoneScreenshots"
    static def IMAGE_TYPE_PROMO_GRAPHIC = "promoGraphic"
    static def IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS = "sevenInchScreenshots"
    static def IMAGE_TYPE_TEN_INCH_SCREENSHOTS = "tenInchScreenshots"

    File inputFolder

    @TaskAction
    publishListing() {
        super.publish()

        // Matches if locale have the correct naming e.g. en-US for play store
        inputFolder.eachDirMatch(matcher) { dir ->
            def locale = dir.getName()

            File listingDir = new File(dir, LISTING_PATH)
            // Check if listing directory exist
            if (listingDir.exists()) {
                File fileTitle = new File(listingDir, FILE_NAME_FOR_TITLE)
                def title = TaskHelper.readAndTrimFile(fileTitle, MAX_CHARACTER_LENGTH_FOR_TITLE)

                File fileShortDescription = new File(listingDir, FILE_NAME_FOR_SHORT_DESCRIPTION)
                def shortDescription = TaskHelper.readAndTrimFile(fileShortDescription, MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION)

                File fileFullDescription = new File(listingDir, FILE_NAME_FOR_FULL_DESCRIPTION)
                def fullDescription = TaskHelper.readAndTrimFile(fileFullDescription, MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION)

                final Listing listing = new Listing()
                if (StringUtils.isNotEmpty(title)) {
                    listing.setTitle(title)
                }
                if (StringUtils.isNotEmpty(shortDescription)) {
                    listing.setShortDescription(shortDescription)
                }
                if (StringUtils.isNotEmpty(fullDescription)) {
                    listing.setFullDescription(fullDescription)
                }

                try {
                    edits.listings()
                            .update(applicationId, editId, locale, listing)
                            .execute()
                } catch (GoogleJsonResponseException e) {

                    // In case we are using an unsupported locale Google generates an error that
                    // is not exactly helpful. In that case we just wrap the original exception in our own.
                    if (e.getMessage() != null && e.getMessage().contains("unsupportedListingLanguage")) {
                        throw new IllegalArgumentException("Unsupported locale " + locale, e);
                    }

                    // Just rethrow everything else.
                    throw e;
                }

                // By default this will be skipped – can be enabled in the play extension
                if (extension.uploadImages) {
                    // Only one ContentFile allow for featureGraphic
                    AbstractInputStreamContent featureGraphicContent = TaskHelper.getImageAsStream(listingDir, IMAGE_TYPE_FEATURE_GRAPHIC + "/")
                    uploadSingleGraphic(featureGraphicContent, locale, IMAGE_TYPE_FEATURE_GRAPHIC)

                    // Only one ContentFile allow for iconGraphic
                    AbstractInputStreamContent iconGraphicContent = TaskHelper.getImageAsStream(listingDir, IMAGE_TYPE_ICON + "/")
                    uploadSingleGraphic(iconGraphicContent, locale, IMAGE_TYPE_ICON)

                    // Only one ContentFile allow for promoGraphic
                    AbstractInputStreamContent promoGraphicContent = TaskHelper.getImageAsStream(listingDir, IMAGE_TYPE_PROMO_GRAPHIC + "/")
                    uploadSingleGraphic(promoGraphicContent, locale, IMAGE_TYPE_PROMO_GRAPHIC)

                    // Upload phoneScreenshots
                    List<AbstractInputStreamContent> phoneContentList = TaskHelper.getImageListAsStream(listingDir, IMAGE_TYPE_PHONE_SCREENSHOTS + "/")
                    uploadScreenshots(phoneContentList, locale, IMAGE_TYPE_PHONE_SCREENSHOTS)

                    // Upload sevenInchScreenshots
                    List<AbstractInputStreamContent> sevenInchContentList = TaskHelper.getImageListAsStream(listingDir, IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS + "/")
                    uploadScreenshots(sevenInchContentList, locale, IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS)

                    // Upload tenInchScreenshots
                    List<AbstractInputStreamContent> tenInchContentList = TaskHelper.getImageListAsStream(listingDir, IMAGE_TYPE_TEN_INCH_SCREENSHOTS + "/")
                    uploadScreenshots(tenInchContentList, locale, IMAGE_TYPE_TEN_INCH_SCREENSHOTS)
                }
            }
        }

        edits.commit(applicationId, editId).execute()
    }

    def uploadSingleGraphic(AbstractInputStreamContent contentGraphic, String locale, String imageType) {
        if (contentGraphic != null) {
            AndroidPublisher.Edits.Images images = edits.images()

            // Delete current image in play store
            images.deleteall(applicationId, editId, locale, imageType).execute()

            // After that upload the new image
            images.upload(applicationId, editId, locale, imageType, contentGraphic).execute()
        }
    }

    def uploadScreenshots(List<AbstractInputStreamContent> contentGraphicList, String locale, String imageType) {
        if (contentGraphicList != null) {
            AndroidPublisher.Edits.Images images = edits.images()

            // Delete all images in play store
            images.deleteall(applicationId, editId, locale, imageType).execute()

            // After that upload the new images
            if (contentGraphicList.size() > MAX_SCREENSHOTS_SIZE) {
                logger.error("Sorry! You can only upload 8 screenshots")
            } else {
                contentGraphicList.each { contentGraphic ->
                    images.upload(applicationId, editId, locale, imageType, contentGraphic).execute()
                }
            }
        }
    }

}

