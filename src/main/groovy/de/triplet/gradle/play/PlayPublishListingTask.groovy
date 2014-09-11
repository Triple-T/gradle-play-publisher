package de.triplet.gradle.play

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Listing
import org.apache.commons.lang.StringUtils
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
            }
            AndroidPublisher.Edits.Commit commitRequest = edits.commit(applicationId, editId);
            commitRequest.execute();
        }
    }

    void setExtension(PlayPublisherPluginExtension extension) {
        this.extension = extension
    }

}

