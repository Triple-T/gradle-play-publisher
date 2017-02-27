package de.triplet.gradle.play

import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.Image
import com.google.api.services.androidpublisher.model.Listing
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction

class BootstrapTask extends PlayPublishTask {

    def IMAGE_TYPE_ARRAY = [
            PlayPublishListingTask.IMAGE_TYPE_ICON,
            PlayPublishListingTask.IMAGE_TYPE_FEATURE_GRAPHIC,
            PlayPublishListingTask.IMAGE_TYPE_PHONE_SCREENSHOTS,
            PlayPublishListingTask.IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS,
            PlayPublishListingTask.IMAGE_TYPE_TEN_INCH_SCREENSHOTS,
            PlayPublishListingTask.IMAGE_TYPE_PROMO_GRAPHIC,
            PlayPublishListingTask.IMAGE_TYPE_TV_BANNER,
            PlayPublishListingTask.IMAGE_TYPE_TV_SCREENSHOTS,
            PlayPublishListingTask.IMAGE_TYPE_WEAR_SCREENSHOTS,
    ]

    File outputFolder

    @TaskAction
    bootstrap() {
        super.publish()

        bootstrapListing()
        bootstrapWhatsNew()
        bootstrapAppDetails()
    }

    def bootstrapListing() {
        def listings = edits.listings()
                .list(variant.applicationId, editId)
                .execute()
                .getListings()
        if (listings == null) {
            return
        }

        for (Listing listing : listings) {
            def language = listing.getLanguage()
            def fullDescription = listing.getFullDescription()
            def shortDescription = listing.getShortDescription()
            def title = listing.getTitle()
            def video = listing.getVideo()

            def languageDir = new File(outputFolder, language)
            if (!languageDir.exists() && !languageDir.mkdirs()) {
                continue
            }

            def listingDir = new File(languageDir, PlayPublishListingTask.LISTING_PATH)
            if (!listingDir.exists() && !listingDir.mkdirs()) {
                continue
            }

            for (String imageType : IMAGE_TYPE_ARRAY) {
                def images = edits.images()
                        .list(variant.applicationId, editId, language, imageType)
                        .execute()
                        .getImages()
                saveImage(listingDir, imageType, images)
            }

            FileUtils.writeStringToFile(new File(listingDir, PlayPublishListingTask.FILE_NAME_FOR_FULL_DESCRIPTION), fullDescription, 'UTF-8')
            FileUtils.writeStringToFile(new File(listingDir, PlayPublishListingTask.FILE_NAME_FOR_SHORT_DESCRIPTION), shortDescription, 'UTF-8')
            FileUtils.writeStringToFile(new File(listingDir, PlayPublishListingTask.FILE_NAME_FOR_TITLE), title, 'UTF-8')
            FileUtils.writeStringToFile(new File(listingDir, PlayPublishListingTask.FILE_NAME_FOR_VIDEO), video, 'UTF-8')
        }
    }

    def bootstrapWhatsNew() {
        def apks = edits.apks()
                .list(variant.applicationId, editId)
                .execute()
                .getApks()
        if (apks == null) {
            return
        }
        def versionCode = apks.collect { apk -> apk.getVersionCode() }.max()

        def apkListings = edits.apklistings()
                .list(variant.applicationId, editId, versionCode)
                .execute()
                .getListings()
        if (apkListings == null) {
            return
        }

        for (ApkListing apkListing : apkListings) {
            def language = apkListing.getLanguage()
            def whatsNew = apkListing.getRecentChanges()

            def languageDir = new File(outputFolder, language)
            if (!languageDir.exists() && !languageDir.mkdirs()) {
                continue
            }

            FileUtils.writeStringToFile(new File(languageDir, PlayPublishApkTask.FILE_NAME_FOR_WHATS_NEW_TEXT), whatsNew, 'UTF-8')
        }
    }

    def bootstrapAppDetails() {
        def appDetails = edits.details().get(variant.applicationId, editId).execute()

        FileUtils.writeStringToFile(new File(outputFolder, PlayPublishListingTask.FILE_NAME_FOR_CONTACT_EMAIL), appDetails.getContactEmail())
        FileUtils.writeStringToFile(new File(outputFolder, PlayPublishListingTask.FILE_NAME_FOR_CONTACT_PHONE), appDetails.getContactPhone())
        FileUtils.writeStringToFile(new File(outputFolder, PlayPublishListingTask.FILE_NAME_FOR_CONTACT_WEBSITE), appDetails.getContactWebsite())
        FileUtils.writeStringToFile(new File(outputFolder, PlayPublishListingTask.FILE_NAME_FOR_DEFAULT_LANGUAGE), appDetails.getDefaultLanguage())
    }

    static def saveImage(File listingDir, String imageFolderName, List<Image> images) {
        def imageFolder = new File(listingDir, imageFolderName)
        if (!imageFolder.exists() && !imageFolder.mkdirs()) {
            return
        }

        if (images == null) {
            return
        }

        // TODO: Disabled for now as we have only access to preview-versions with the current API.
        /*
        for (Image image : images) {
            try {
                OutputStream os = new File(imageFolder, image.getId() + '.png').newOutputStream()
                os << image.getUrl().toURL().openStream()
                os.close()
            } catch (IOException e) {
                e.printStackTrace()
            }
        }
        */
    }

}
