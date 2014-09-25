package de.triplet.gradle.play

import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Listing
import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

class PlayPublishListingTask extends PlayPublishTask {

    def MAX_CHARACTER_LENGTH_FOR_TITLE = 30
    def MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION = 80
    def MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION = 4000
    def MAX_SCREESHOTS_SIZE = 8

    def FILE_NAME_FOR_TITLE = "title"
    def FILE_NAME_FOR_SHORT_DESCRIPTION = "shortdescription"
    def FILE_NAME_FOR_FULL_DESCRIPTION = "fulldescription"
    def LISTING_PATH = "listing/"

    def IMAGE_TYPE_FEATURE_GRAPHIC = "featureGraphic"
    def IMAGE_TYPE_ICON = "icon"
    def IMAGE_TYPE_PHONE_SCREENSHOTS = "phoneScreenshots"
    def IMAGE_TYPE_PROMO_GRAPHIC = "promoGraphic"
    def IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS = "sevenInchScreenshots"
    def IMAGE_TYPE_TEN_INCH_SCREENSHOTS = "tenInchScreenshots"

    def matcher = ~"^[a-z]{2}(-[A-Z]{2})?\\z"

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

                final Listing listing = new Listing();
                if (!StringUtils.isEmpty(title)) {
                    listing.setTitle(title);
                }
                if (!StringUtils.isEmpty(shortDescription)) {
                    listing.setShortDescription(shortDescription);
                }
                if (!StringUtils.isEmpty(fullDescription)) {
                    listing.setFullDescription(fullDescription);
                }

                AndroidPublisher.Edits.Listings.Update updateListingsRequest = edits
                        .listings()
                        .update(applicationId,
                        editId, locale, listing);
                updateListingsRequest.execute();

                // By default this value is false , optional you can set this value of true in play extension
                def uploadImages = extension.uploadImages

                if (uploadImages) {
                    // delete all images from play store entry for each locale
                    // after that upload the new one

                    //Only one ContentFile allow for featureGraphic
                    AbstractInputStreamContent featureGraphicContent = TaskHelper.getAbtractInputStreamContentFile(listingDir, IMAGE_TYPE_FEATURE_GRAPHIC + "/")
                    uploadSingleGraphic(featureGraphicContent, locale, IMAGE_TYPE_FEATURE_GRAPHIC)

                    //Only one ContentFile allow for iconGraphic
                    AbstractInputStreamContent iconGraphicContent = TaskHelper.getAbtractInputStreamContentFile(listingDir, IMAGE_TYPE_ICON + "/")
                    uploadSingleGraphic(iconGraphicContent, locale, IMAGE_TYPE_ICON)

                    //Only one ContentFile allow for promoGraphic
                    AbstractInputStreamContent promoGraphicContent = TaskHelper.getAbtractInputStreamContentFile(listingDir, IMAGE_TYPE_PROMO_GRAPHIC + "/")
                    uploadSingleGraphic(promoGraphicContent, locale, IMAGE_TYPE_PROMO_GRAPHIC)

                    //Upload phoneScreenshots
                    List<AbstractInputStreamContent> phoneContentList = TaskHelper.getAbstractInputStreamContentList(listingDir, IMAGE_TYPE_PHONE_SCREENSHOTS + "/")
                    uploadScreenshots(phoneContentList, locale, IMAGE_TYPE_PHONE_SCREENSHOTS)

                    //Upload sevenInchScreenshots
                    List<AbstractInputStreamContent> sevenInchContentList = TaskHelper.getAbstractInputStreamContentList(listingDir, IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS + "/")
                    uploadScreenshots(sevenInchContentList, locale, IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS)

                    //Upload tenInchScreenshots
                    List<AbstractInputStreamContent> tenInchContentList = TaskHelper.getAbstractInputStreamContentList(listingDir, IMAGE_TYPE_TEN_INCH_SCREENSHOTS + "/")
                    uploadScreenshots(tenInchContentList, locale, IMAGE_TYPE_TEN_INCH_SCREENSHOTS)
                }
            }
            AndroidPublisher.Edits.Commit commitRequest = edits.commit(applicationId, editId);
            commitRequest.execute();
        }
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
        AndroidPublisher.Edits.Images images = edits.images()


        if (contentGraphicList != null) {
            // delete all images in play store before upload new images 
            images.deleteall(applicationId, editId, locale, imageType).execute()

            if (contentGraphicList.size() > MAX_SCREESHOTS_SIZE) {
                logger.info("Sorry! You could only upload 8 screenshots  ")
            } else {
                contentGraphicList.each { contentGraphic ->
                    images.upload(applicationId, editId, locale, imageType, contentGraphic).execute()
                }
            }
        }
    }

}

