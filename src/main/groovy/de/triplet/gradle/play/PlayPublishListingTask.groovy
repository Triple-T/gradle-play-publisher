package de.triplet.gradle.play

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
import org.gradle.api.tasks.TaskAction

class PlayPublishListingTask extends PlayPublishTask {

    static final MAX_CHARACTER_LENGTH_FOR_TITLE = 50
    static final MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION = 80
    static final MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION = 4000
    static final MAX_SCREENSHOTS_SIZE = 8

    static final FILE_NAME_FOR_CONTACT_EMAIL = 'contactEmail'
    static final FILE_NAME_FOR_CONTACT_PHONE = 'contactPhone'
    static final FILE_NAME_FOR_CONTACT_WEBSITE = 'contactWebsite'
    static final FILE_NAME_FOR_DEFAULT_LANGUAGE = 'defaultLanguage'

    static final FILE_NAME_FOR_TITLE = 'title'
    static final FILE_NAME_FOR_SHORT_DESCRIPTION = 'shortdescription'
    static final FILE_NAME_FOR_FULL_DESCRIPTION = 'fulldescription'
    static final FILE_NAME_FOR_VIDEO = 'video'
    static final LISTING_PATH = 'listing/'

    static final IMAGE_TYPE_FEATURE_GRAPHIC = 'featureGraphic'
    static final IMAGE_TYPE_ICON = 'icon'
    static final IMAGE_TYPE_PHONE_SCREENSHOTS = 'phoneScreenshots'
    static final IMAGE_TYPE_PROMO_GRAPHIC = 'promoGraphic'
    static final IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS = 'sevenInchScreenshots'
    static final IMAGE_TYPE_TEN_INCH_SCREENSHOTS = 'tenInchScreenshots'
    static final IMAGE_TYPE_TV_BANNER = 'tvBanner'
    static final IMAGE_TYPE_TV_SCREENSHOTS = 'tvScreenshots'
    static final IMAGE_TYPE_WEAR_SCREENSHOTS = 'wearScreenshots'

    File inputFolder

    @TaskAction
    publishListing() {
        if (!inputFolder.exists()) {
            return
        }

        publish()

        // Matches if locale have the correct naming e.g. en-US for play store
        inputFolder.eachDirMatch(matcher) { dir ->
            def locale = dir.name

            def listingDir = new File(dir, LISTING_PATH)
            // Check if listing directory exist
            if (listingDir.exists()) {
                def fileTitle = new File(listingDir, FILE_NAME_FOR_TITLE)
                def fileShortDescription = new File(listingDir, FILE_NAME_FOR_SHORT_DESCRIPTION)
                def fileFullDescription = new File(listingDir, FILE_NAME_FOR_FULL_DESCRIPTION)
                def fileVideo = new File(listingDir, FILE_NAME_FOR_VIDEO)

                def title = TaskHelper.readAndTrimFile(project, fileTitle, MAX_CHARACTER_LENGTH_FOR_TITLE, extension.errorOnSizeLimit)
                def shortDescription = TaskHelper.readAndTrimFile(project, fileShortDescription, MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION, extension.errorOnSizeLimit)
                def fullDescription = TaskHelper.readAndTrimFile(project, fileFullDescription, MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION, extension.errorOnSizeLimit)
                def video = TaskHelper.readSingleLine(fileVideo)

                final listing = new Listing()
                if (!title?.isEmpty()) {
                    listing.setTitle(title)
                }
                if (!shortDescription?.isEmpty()) {
                    listing.setShortDescription(shortDescription)
                }
                if (!fullDescription?.isEmpty()) {
                    listing.setFullDescription(fullDescription)
                }
                if (!video?.isEmpty()) {
                    listing.setVideo(video)
                }

                try {
                    edits.listings()
                            .update(variant.applicationId, editId, locale, listing)
                            .execute()
                } catch (GoogleJsonResponseException e) {

                    // In case we are using an unsupported locale Google generates an error that
                    // is not exactly helpful. In that case we just wrap the original exception in our own.
                    if (e.message != null && e.message.contains('unsupportedListingLanguage')) {
                        throw new IllegalArgumentException("Unsupported locale ${locale}", e)
                    }

                    // Just rethrow everything else.
                    throw e
                }

                // By default this will be skipped – can be enabled in the play extension
                if (extension.uploadImages) {
                    // Only one ContentFile allowed for featureGraphic
                    def featureGraphicContent = TaskHelper.getImageAsStream(listingDir, IMAGE_TYPE_FEATURE_GRAPHIC + '/')
                    uploadSingleGraphic(featureGraphicContent, locale, IMAGE_TYPE_FEATURE_GRAPHIC)

                    // Only one ContentFile allowed for iconGraphic
                    def iconGraphicContent = TaskHelper.getImageAsStream(listingDir, IMAGE_TYPE_ICON + '/')
                    uploadSingleGraphic(iconGraphicContent, locale, IMAGE_TYPE_ICON)

                    // Only one ContentFile allowed for promoGraphic
                    def promoGraphicContent = TaskHelper.getImageAsStream(listingDir, IMAGE_TYPE_PROMO_GRAPHIC + '/')
                    uploadSingleGraphic(promoGraphicContent, locale, IMAGE_TYPE_PROMO_GRAPHIC)

                    // Only one ContentFile allowed for tvBanner
                    def tvBannerGraphicContent = TaskHelper.getImageAsStream(listingDir, IMAGE_TYPE_TV_BANNER + '/')
                    uploadSingleGraphic(tvBannerGraphicContent, locale, IMAGE_TYPE_TV_BANNER)

                    // Upload phoneScreenshots
                    def phoneContentList = TaskHelper.getImageListAsStream(listingDir, IMAGE_TYPE_PHONE_SCREENSHOTS + '/')
                    uploadScreenshots(phoneContentList, locale, IMAGE_TYPE_PHONE_SCREENSHOTS)

                    // Upload sevenInchScreenshots
                    def sevenInchContentList = TaskHelper.getImageListAsStream(listingDir, IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS + '/')
                    uploadScreenshots(sevenInchContentList, locale, IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS)

                    // Upload tenInchScreenshots
                    def tenInchContentList = TaskHelper.getImageListAsStream(listingDir, IMAGE_TYPE_TEN_INCH_SCREENSHOTS + '/')
                    uploadScreenshots(tenInchContentList, locale, IMAGE_TYPE_TEN_INCH_SCREENSHOTS)

                    // Upload tvScreenshots
                    def tvContentList = TaskHelper.getImageListAsStream(listingDir, IMAGE_TYPE_TV_SCREENSHOTS + '/')
                    uploadScreenshots(tvContentList, locale, IMAGE_TYPE_TV_SCREENSHOTS)

                    // Upload wearScreenshots
                    def wearContentList = TaskHelper.getImageListAsStream(listingDir, IMAGE_TYPE_WEAR_SCREENSHOTS + '/')
                    uploadScreenshots(wearContentList, locale, IMAGE_TYPE_WEAR_SCREENSHOTS)
                }
            }
        }

        def fileDefaultLanguage = new File(inputFolder, FILE_NAME_FOR_DEFAULT_LANGUAGE)
        def defaultLanguage = TaskHelper.readSingleLine(fileDefaultLanguage)

        if (!defaultLanguage?.isEmpty()) {
            def fileContactEmail = new File(inputFolder, FILE_NAME_FOR_CONTACT_EMAIL)
            def email = TaskHelper.readSingleLine(fileContactEmail)
            def fileContactPhone = new File(inputFolder, FILE_NAME_FOR_CONTACT_PHONE)
            def phone = TaskHelper.readSingleLine(fileContactPhone)
            def fileContactWeb = new File(inputFolder, FILE_NAME_FOR_CONTACT_WEBSITE)
            def web = TaskHelper.readSingleLine(fileContactWeb)

            def details = new AppDetails()

            details.setContactEmail(email)
                    .setContactPhone(phone)
                    .setContactWebsite(web)
                    .setDefaultLanguage(defaultLanguage)

            edits.details()
                    .update(variant.applicationId, editId, details)
                    .execute()
        }


        edits.commit(variant.applicationId, editId).execute()
    }

    def uploadSingleGraphic(AbstractInputStreamContent contentGraphic, String locale, String imageType) {
        if (contentGraphic != null) {
            def images = edits.images()

            // Delete current image in play store
            images.deleteall(variant.applicationId, editId, locale, imageType).execute()

            // After that upload the new image
            images.upload(variant.applicationId, editId, locale, imageType, contentGraphic).execute()
        }
    }

    def uploadScreenshots(List<AbstractInputStreamContent> contentGraphicList, String locale, String imageType) {
        if (contentGraphicList != null) {
            def images = edits.images()

            // Delete all images in play store
            images.deleteall(variant.applicationId, editId, locale, imageType).execute()

            // After that upload the new images
            if (contentGraphicList.size() > MAX_SCREENSHOTS_SIZE) {
                logger.error('Sorry! You can only upload 8 screenshots')
            } else {
                contentGraphicList.each { contentGraphic ->
                    images.upload(variant.applicationId, editId, locale, imageType, contentGraphic).execute()
                }
            }
        }
    }
}

