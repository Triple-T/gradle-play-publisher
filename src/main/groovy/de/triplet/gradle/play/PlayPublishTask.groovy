package de.triplet.gradle.play

import com.android.builder.core.DefaultManifestParser
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.AppEdit
import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

class PlayPublishTask extends DefaultTask {

    PlayPublisherPluginExtension extension

    @Input
    File manifestFile

    String applicationId
    String editId

    AndroidPublisher.Edits edits

    def publish() {

        applicationId = new DefaultManifestParser().getPackage(manifestFile)

        final AndroidPublisher service = AndroidPublisherHelper.init(extension.serviceAccountEmail, extension.pk12File)

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
