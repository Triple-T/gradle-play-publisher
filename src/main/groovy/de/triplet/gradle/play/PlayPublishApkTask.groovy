package de.triplet.gradle.play

import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.Track
import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

class PlayPublishApkTask extends PlayPublishTask {

    def MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT = 500
    def FILE_NAME_FOR_WHATS_NEW_TEXT = "whatsnew"

    @Input
    File apkFile

    @InputDirectory
    File inputFolder

    @TaskAction
    publishApk() {
        super.publish()

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

        // Matches if locale have the correct naming e.g. en-US for play store
        inputFolder.eachDirMatch(matcher) { dir ->

            File whatsNewFile = new File(dir.getAbsolutePath(), FILE_NAME_FOR_WHATS_NEW_TEXT)
            def whatsNewText = TaskHelper.readAndTrimFile(whatsNewFile, MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT)

            if (!StringUtils.isEmpty(whatsNewText)) {
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

}
