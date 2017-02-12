package de.triplet.gradle.play

import com.android.build.gradle.api.ApplicationVariant
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.AppEdit
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TracksListResponse
import org.gradle.api.DefaultTask

class PlayPublishTask extends DefaultTask {

    // region '419' is a special case in the play store that represents latin america
    // 'fil' is a special case in the play store that represents Filipino
    def matcher = ~"^(fil|[a-z]{2}(-([A-Z]{2}|419))?)\\z"

    PlayPublisherPluginExtension extension

    ApplicationVariant variant

    String editId

    AndroidPublisher service

    AndroidPublisher.Edits edits

    def publish() {
        if (service == null) {
            service = AndroidPublisherHelper.init(extension)
        }

        edits = service.edits()

        // Create a new edit to make changes to your listing.
        AndroidPublisher.Edits.Insert editRequest = edits.insert(variant.applicationId, null /* no content yet */)

        try {
            AppEdit edit = editRequest.execute()
            editId = edit.getId()
        } catch (GoogleJsonResponseException e) {

            // The very first release has to be uploaded via the web interface.
            // We add a little explanation to Google's exception.
            if (e.message != null && e.message.contains("applicationNotFound")) {
                throw new IllegalArgumentException("No application was found for the package name " + variant.applicationId + ". Is this the first release for this app? The first version has to be uploaded via the web interface.", e);
            }

            // Just rethrow everything else.
            throw e;
        }
    }

    def getTrackByName(String trackName) {
        TracksListResponse tracksList = edits.tracks().list(variant.applicationId, editId).execute()
        for (Track track in tracksList.getTracks()) {
            if (trackName.equals(track.getTrack())) {
                return track
            }
        }
        return null
    }

}
