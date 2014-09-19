package de.triplet.gradle.play

import com.android.builder.core.DefaultManifestParser
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.AppEdit
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input

class PlayPublishTask extends DefaultTask {

    PlayPublisherPluginExtension extension

    @Input
    File manifestFile

    String applicationId
    String editId

    AndroidPublisher service

    AndroidPublisher.Edits edits

    def publish() {

        applicationId = new DefaultManifestParser().getPackage(manifestFile)

        if (service == null) {
            service = AndroidPublisherHelper.init(extension.serviceAccountEmail, extension.pk12File)
        }

        edits = service.edits();

        // Create a new edit to make changes to your listing.
        AndroidPublisher.Edits.Insert editRequest = edits.insert(
                applicationId,
                null /** no content yet */);
        AppEdit edit = editRequest.execute();

        editId = edit.getId();
    }

    void setExtension(PlayPublisherPluginExtension extension) {
        this.extension = extension
    }

}
