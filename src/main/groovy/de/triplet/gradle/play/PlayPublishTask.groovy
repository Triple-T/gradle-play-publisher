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

    private PlayPublisherPluginExtension extension

    @Input
    File apkFile

    @Input
    File manifestFile

    @InputDirectory
    File inputFolder

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
                new FileContent(AndroidPublisherHelper.MIME_TYPE_APK, apkFile);


        AndroidPublisher.Edits.Apks.Upload uploadRequest = edits
                .apks()
                .upload(applicationId, editId, apkFile);

        Apk apk = uploadRequest.execute();

        List<Integer> apkVersionCodes = new ArrayList<>();
        apkVersionCodes.add(apk.getVersionCode());
        AndroidPublisher.Edits.Tracks.Update updateTrackRequest = edits
                .tracks()
                .update(applicationId, editId, extension.track, new Track().setVersionCodes(apkVersionCodes));
        updateTrackRequest.execute();

        inputFolder.eachDirRecurse { dir ->
            File file = new File(dir.getAbsolutePath().toString() + "/whatsnew")
            if (file.exists()) {

                def whatsNewText = file.text
                def locale = dir.getName()
                ApkListing newApkListing = new ApkListing();
                newApkListing.setRecentChanges(whatsNewText);

                AndroidPublisher.Edits.Apklistings.Update updateRecentChangesRequest = edits
                        .apklistings()
                        .update(applicationId,
                        editId,
                        apk.getVersionCode(),
                        locale,
                        newApkListing);

                updateRecentChangesRequest.execute();
            }
        }

        AndroidPublisher.Edits.Commit commitRequest = edits.commit(applicationId, editId);
        commitRequest.execute();
    }

    void setExtension(PlayPublisherPluginExtension extension) {
        this.extension = extension
    }

}
