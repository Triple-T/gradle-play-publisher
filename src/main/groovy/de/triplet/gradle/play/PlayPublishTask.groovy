package de.triplet.gradle.play

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.AppEdit
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PlayPublishTask extends DefaultTask {

    private PlayPublisherPluginExtension extension

    @TaskAction
    def publish() {

        AndroidPublisher service = AndroidPublisherHelper.init(
                extension.applicationName, extension.serviceAccountEmail, extension.pk12File)

        final AndroidPublisher.Edits edits = service.edits();

        // Create a new edit to make changes to your listing.
        AndroidPublisher.Edits.Insert editRequest = edits.insert(
                extension.applicationId,
                null /** no content yet */);
        AppEdit edit = editRequest.execute();

        final String editId = edit.getId();
        println editId
    }

    void setExtension(PlayPublisherPluginExtension extension) {
        this.extension = extension
    }

}
