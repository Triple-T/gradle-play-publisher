package de.triplet.gradle.play

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Listing
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

class PlayPublishListingTask extends PlayPublishTask {

    private PlayPublisherPluginExtension extension

    def MAX_CHARACTER_LENGTH_FOR_TITLE = 30
    def MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION = 80
    def MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION = 4000

    def FILE_NAME_FOR_TITLE = "title"
    def FILE_NAME_FOR_SHORT_DESCRIPTION = "shortdescription"
    def FILE_NAME_FOR_FULL_DESCRIPTION = "fulldescription"
    def LISTING_PATH = "listing/"

    @InputDirectory
    File inputFolder

    @TaskAction
    publishListing() {
        super.publish()

        inputFolder.eachDirRecurse { dir ->
            File fileTitle = new File(dir, LISTING_PATH + FILE_NAME_FOR_TITLE)
            def title = fileTitle.text
            if (title.length() > MAX_CHARACTER_LENGTH_FOR_TITLE) {
                title.substring(0, MAX_CHARACTER_LENGTH_FOR_TITLE)
                logger.info("Check your title because it is to long for upload, the plugin cut your title automatically!")
            }

            File fileShortDescription = new File(dir, LISTING_PATH + FILE_NAME_FOR_SHORT_DESCRIPTION)
            def shortDescription = fileShortDescription.text
            if (shortDescription.length() > MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION) {
                shortDescription.substring(0, MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION)
                logger.info("Check your shortdescription because it is to long for upload, the plugin cut your shortdescription automatically!")
            }

            File fileFullDescription = new File(dir, LISTING_PATH + FILE_NAME_FOR_FULL_DESCRIPTION)
            def fullDescription = fileFullDescription.text
            if (fullDescription.length() > MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION) {
                fullDescription.substring(0, MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION)
                logger.info("Check your fulldescription because it is to long for upload, the plugin cut your fulldescription automatically!")
            }

            final Listing listing = new Listing();
            listing.setTitle(title)
                    .setShortDescription(shortDescription)
                    .setFullDescription(fullDescription);

            def locale = dir.getName()

            AndroidPublisher.Edits.Listings.Update updateListingsRequest = edits
                    .listings()
                    .update(applicationId,
                    editId, locale, listing);
            updateListingsRequest.execute();
        }

        AndroidPublisher.Edits.Commit commitRequest = edits.commit(applicationId, editId);
        commitRequest.execute();
    }

    void setExtension(PlayPublisherPluginExtension extension) {
        this.extension = extension
    }

}

