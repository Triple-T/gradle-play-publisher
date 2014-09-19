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

    def PATH_FOR_FEATURE_GRAPHIC = "featureGraphic/"
    def PATH_FOR_ICON = "icon/"
    def PATH_FOR_PHONE_SCREESHOTS = "phoneScreenshots/"
    def PATH_FOR_PROMO_GRAPHIC = "promoGraphic/"
    def PATH_FOR_SEVEN_INCH_SCREENSHOTS = "sevenInchScreenshots/"
    def PATH_FOR_TEN_INCH_SCREENSHOTS = "tenInchScreenshots/"

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

                AndroidPublisher.Edits.Images images = edits.images()
                //Only one ContentFile allow for featureGraphic
                AbstractInputStreamContent featureGraphicContent = TaskHelper.getAbtractInputStreamContentFile(listingDir, PATH_FOR_FEATURE_GRAPHIC)
                if (featureGraphicContent != null) {
                    images.upload(applicationId, editId, locale, IMAGE_TYPE_FEATURE_GRAPHIC, featureGraphicContent).execute()
                }

                //Only one ContentFile allow for iconGraphic
                AbstractInputStreamContent iconGraphicContent = TaskHelper.getAbtractInputStreamContentFile(listingDir, PATH_FOR_ICON)
                if (iconGraphicContent != null) {
                    images.upload(applicationId, editId, locale, IMAGE_TYPE_ICON, iconGraphicContent).execute()
                }

                //Only one ContentFile allow for promoGraphic
                AbstractInputStreamContent promoGraphicContent = TaskHelper.getAbtractInputStreamContentFile(listingDir, PATH_FOR_PROMO_GRAPHIC)
                if (promoGraphicContent != null) {
                    images.upload(applicationId, editId, locale, IMAGE_TYPE_PROMO_GRAPHIC, promoGraphicContent).execute()
                }

                //Upload phoneScreenshots
                List<AbstractInputStreamContent> phoneContentList = TaskHelper.getAbstractInputStreamContentList(listingDir, PATH_FOR_PHONE_SCREESHOTS)
                if (phoneContentList != null) {
                    if (phoneContentList.size() > MAX_SCREESHOTS_SIZE) {
                        logger.info("Sorry! You could only upload 8 screenshots  ")
                    } else {
                        phoneContentList.each { phoneContentGraphic ->
                            images.upload(applicationId, editId, locale, IMAGE_TYPE_PHONE_SCREENSHOTS, phoneContentGraphic).execute()
                        }
                    }
                }

                //Upload sevenInchScreenshots
                List<AbstractInputStreamContent> sevenInchContentList = TaskHelper.getAbstractInputStreamContentList(listingDir, PATH_FOR_SEVEN_INCH_SCREENSHOTS)
                if (sevenInchContentList != null) {
                    if (sevenInchContentList.size() > MAX_SCREESHOTS_SIZE) {
                        logger.info("Sorry! You could only upload 8 screenshots  ")
                    } else {
                        sevenInchContentList.each { sevenInchContentGraphic ->
                            images.upload(applicationId, editId, locale, IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS, sevenInchContentGraphic).execute()
                        }
                    }
                }

                //Upload tenInchScreenshots
                List<AbstractInputStreamContent> tenInchContentList = TaskHelper.getAbstractInputStreamContentList(listingDir, PATH_FOR_TEN_INCH_SCREENSHOTS)
                if (tenInchContentList != null) {
                    if (tenInchContentList.size() > MAX_SCREESHOTS_SIZE) {
                        logger.info("Sorry! You could only upload 8 screenshots  ")
                    } else {
                        tenInchContentList.each { tenInchContentGraphic ->
                            images.upload(applicationId, editId, locale, IMAGE_TYPE_TEN_INCH_SCREENSHOTS, tenInchContentGraphic).execute()
                        }
                    }
                }
            }
            AndroidPublisher.Edits.Commit commitRequest = edits.commit(applicationId, editId);
            commitRequest.execute();
        }
    }

}

