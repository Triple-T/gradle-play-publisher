package de.triplet.gradle.play

import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.Image
import com.google.api.services.androidpublisher.model.Listing
import com.google.api.services.androidpublisher.model.Track
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction

class BootstrapTask extends PlayPublishTask {

    def IMAGE_TYPE_ARRAY = [
            PlayPublishListingTask.IMAGE_TYPE_ICON,
            PlayPublishListingTask.IMAGE_TYPE_FEATURE_GRAPHIC,
            PlayPublishListingTask.IMAGE_TYPE_PHONE_SCREENSHOTS,
            PlayPublishListingTask.IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS,
            PlayPublishListingTask.IMAGE_TYPE_TEN_INCH_SCREENSHOTS,
            PlayPublishListingTask.IMAGE_TYPE_PROMO_GRAPHIC
    ]

    File outputFolder

    @TaskAction
    bootstrap() {
        super.publish()

        bootstrapListing()
        bootstrapWhatsNew()
    }

    def bootstrapListing() {
        List<Listing> listings = edits.listings()
                .list(variant.getApplicationId(), editId)
                .execute()
                .getListings()
        if (listings == null) {
            return
        }

        String language
        String fullDescription
        String shortDescription
        String title
        String video

        edits.tracks().list(variant.getApplicationId(), editId).execute().tracks.each { Track track ->
            FileUtils.writeStringToFile(new File(outputFolder,
                    "${PlayPublishListingTask.FILE_NAME_FOR_VERSION}-${track.track}"), track.versionCodes.join(','), 'UTF-8')
        }

        for (Listing listing : listings) {
            language = listing.getLanguage()
            fullDescription = listing.getFullDescription()
            shortDescription = listing.getShortDescription()
            title = listing.getTitle()
            video = listing.getVideo()

            File languageDir = new File(outputFolder, language)
            if (!languageDir.exists() && !languageDir.mkdirs()) {
                continue
            }

            File listingDir = new File(languageDir, PlayPublishListingTask.LISTING_PATH)
            if (!listingDir.exists() && !listingDir.mkdirs()) {
                continue
            }

            for (String imageType : IMAGE_TYPE_ARRAY) {
                List<Image> images = edits.images()
                        .list(variant.getApplicationId(), editId, language, imageType)
                        .execute()
                        .getImages()
                saveImage(listingDir, imageType, images)
            }

            FileUtils.writeStringToFile(new File(listingDir, PlayPublishListingTask.FILE_NAME_FOR_FULL_DESCRIPTION), fullDescription, "UTF-8")
            FileUtils.writeStringToFile(new File(listingDir, PlayPublishListingTask.FILE_NAME_FOR_SHORT_DESCRIPTION), shortDescription, "UTF-8")
            FileUtils.writeStringToFile(new File(listingDir, PlayPublishListingTask.FILE_NAME_FOR_TITLE), title, "UTF-8")
            FileUtils.writeStringToFile(new File(listingDir, PlayPublishListingTask.FILE_NAME_FOR_VIDEO), video, "UTF-8")
        }
    }

    def bootstrapWhatsNew() {
        List<Apk> apks = edits.apks()
                .list(variant.getApplicationId(), editId)
                .execute()
                .getApks()
        if (apks == null) {
            return
        }
        Integer versionCode = apks.collect { apk -> apk.getVersionCode() }.max()

        List<ApkListing> apkListings = edits.apklistings()
                .list(variant.getApplicationId(), editId, versionCode)
                .execute()
                .getListings()
        if (apkListings == null) {
            return
        }

        String language
        String whatsNew

        for (ApkListing apkListing : apkListings) {
            language = apkListing.getLanguage()
            whatsNew = apkListing.getRecentChanges()

            File languageDir = new File(outputFolder, language)
            if (!languageDir.exists() && !languageDir.mkdirs()) {
                continue
            }

            FileUtils.writeStringToFile(new File(languageDir, PlayPublishApkTask.FILE_NAME_FOR_WHATS_NEW_TEXT), whatsNew, "UTF-8")
        }
    }

    static def saveImage(File listingDir, String imageFolderName, List<Image> images) {
        File imageFolder = new File(listingDir, imageFolderName)
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
                downloadImageFromUrl(image.getUrl(), new File(imageFolder, image.getId() + ".png"))
            } catch (IOException e) {
                e.printStackTrace()
            }
        }
        */
    }

    /*
    static def downloadImageFromUrl(String imageUrl, File destinationFile) throws IOException {
        InputStream is = imageUrl.toURL().openStream()
        OutputStream os = new FileOutputStream(destinationFile)

        byte[] b = new byte[2048]
        int length

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length)
        }

        is.close()
        os.close()
    }
    */

}
