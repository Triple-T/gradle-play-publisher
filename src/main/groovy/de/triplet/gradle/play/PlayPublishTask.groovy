package de.triplet.gradle.play

import com.android.builder.core.DefaultManifestParser
import com.android.builder.core.ManifestParser
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.AppEdit
import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class PlayPublishTask extends DefaultTask {

    private PlayPublisherPluginExtension extension

    @Input
    File inputFile

    @Input
    File manifestFile

    @TaskAction
    def publish() {

        def applicationId = new DefaultManifestParser().getPackage(manifestFile)

        AndroidPublisher service = AndroidPublisherHelper.init(extension.serviceAccountEmail, extension.pk12File)

        final AndroidPublisher.Edits edits = service.edits();

        // Create a new edit to make changes to your listing.
        AndroidPublisher.Edits.Insert editRequest = edits.insert(
                applicationId,
                null /** no content yet */);
        AppEdit edit = editRequest.execute();

        final String editId = edit.getId();

        final AbstractInputStreamContent apkFile =
                new FileContent(AndroidPublisherHelper.MIME_TYPE_APK, inputFile);


        AndroidPublisher.Edits.Apks.Upload uploadRequest = edits
                .apks()
                .upload(applicationId, editId, apkFile);

        Apk apk = uploadRequest.execute();

        List<Integer> apkVersionCodes = new ArrayList<>();
        apkVersionCodes.add(apk.getVersionCode());
        AndroidPublisher.Edits.Tracks.Update updateTrackRequest = edits
                .tracks()
                .update(applicationId, editId, "alpha", new Track().setVersionCodes(apkVersionCodes));
        updateTrackRequest.execute();

        AndroidPublisher.Edits.Commit commitRequest = edits.commit(applicationId, editId);
        commitRequest.execute();
    }

    void setExtension(PlayPublisherPluginExtension extension) {
        this.extension = extension
    }

}
