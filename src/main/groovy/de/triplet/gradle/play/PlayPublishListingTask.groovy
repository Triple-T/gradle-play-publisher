package de.triplet.gradle.play

import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Listing
import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

class PlayPublishListingTask extends PlayPublishTask {

    static def MAX_CHARACTER_LENGTH_FOR_TITLE = 30
    static def MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION = 80
    static def MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION = 4000
    static def MAX_SCREESHOTS_SIZE = 8

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

    @InputDirectory
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

                AndroidPublisher.Edits.Listings.Update updateListingsRequest = edits
                        .listings()
                        .update(applicationId, editId, locale, listing)
                updateListingsRequest.execute()

                // By default this value is false , optional you can set this value of true in play extension
                def uploadImages = extension.uploadImages

                if (uploadImages) {
                    // delete all images from play store entry for each locale
                    // after that upload the new one

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

        AndroidPublisher.Edits.Commit commitRequest = edits.commit(applicationId, editId)
        commitRequest.execute()
    }


    def uploadSingleGraphic(AbstractInputStreamContent contentGraphic, String locale, String imageType) {
        AndroidPublisher.Edits.Images images = edits.images()


        if (contentGraphic != null) {
            // delete all images in play store before upload new images
            images.deleteall(applicationId, editId, locale, imageType).execute()

            images.upload(applicationId, editId, locale, imageType, contentGraphic).execute()
        }
    }

    def uploadScreenshots(List<AbstractInputStreamContent> contentGraphicList, String locale, String imageType) {
        if (contentGraphicList != null) {
            // delete all images in play store before upload new images 
            AndroidPublisher.Edits.Images images = edits.images()
            images.deleteall(applicationId, editId, locale, imageType).execute()

            if (contentGraphicList.size() > MAX_SCREESHOTS_SIZE) {
                logger.info("Sorry! You could only upload 8 screenshots")
            } else {
                contentGraphicList.each { contentGraphic ->
                    images.upload(applicationId, editId, locale, imageType, contentGraphic).execute()
                }
            }
        }
    }

}

