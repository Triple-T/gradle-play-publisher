package de.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.ExpansionFile
import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class PlayPublishApkTask extends PlayPublishTask {

    static def MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT = 500
    static def FILE_NAME_FOR_WHATS_NEW_TEXT = "whatsnew"

    File inputFolder

    @TaskAction
    publishApk() {
        super.publish()

        def apkOutput = variant.outputs.find { variantOutput -> variantOutput instanceof ApkVariantOutput }
        FileContent newApkFile = new FileContent(AndroidPublisherHelper.MIME_TYPE_APK, apkOutput.outputFile)

        Apk apk = edits.apks()
                .upload(variant.applicationId, editId, newApkFile)
                .execute()

        Track newTrack = new Track().setVersionCodes([apk.getVersionCode()])
        if (extension.track?.equals("rollout")) {
            newTrack.setUserFraction(extension.userFraction)
        }
        edits.tracks()
                .update(variant.applicationId, editId, extension.track, newTrack)
                .execute()

        if (inputFolder.exists()) {

            // Matches if locale have the correct naming e.g. en-US for play store
            inputFolder.eachDirMatch(matcher) { dir ->
                File whatsNewFile = new File(dir, FILE_NAME_FOR_WHATS_NEW_TEXT + "-" + extension.track)

                if (!whatsNewFile.exists()) {
                    whatsNewFile = new File(dir, FILE_NAME_FOR_WHATS_NEW_TEXT)
                }

                if (whatsNewFile.exists()) {

                    def whatsNewText = TaskHelper.readAndTrimFile(whatsNewFile, MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT, extension.errorOnSizeLimit)
                    def locale = dir.name

                    ApkListing newApkListing = new ApkListing().setRecentChanges(whatsNewText)
                    edits.apklistings()
                            .update(variant.applicationId, editId, apk.getVersionCode(), locale, newApkListing)
                            .execute()
                }
            }

        }

        if (extension.uploadObbMain) {
            publishObb("main")
        } else if (extension.associateObbMain > 0) {
            associateObb("main", extension.associateObbMain)
        }

        if (extension.uploadObbPatch) {
            publishObb("patch")
        } else if (extension.associateObbPatch > 0) {
            associateObb("patch", extension.associateObbPatch)
        }

        edits.commit(variant.applicationId, editId).execute()
    }

    private void publishObb(String type) {
        def obbFile = new File(inputFolder, "obb/${type}")

        if (obbFile.exists()) {
            def newObbFile = new FileContent("application/octet-stream", obbFile)

            edits.expansionfiles()
                    .upload(variant.applicationId, editId, variant.versionCode, type, newObbFile)
                    .execute()

            logger.info("Starting upload of the obb file ({} MB), this may take a while",
                    obbFile.length() / 1024 / 1024)

        } else {
            throw new GradleException("Please place a file named `${type}` in the `play/obb/` directory")
        }
    }

    private void associateObb(String type, int version) {
        ExpansionFile content = new ExpansionFile()
        content.setReferencesVersion(version)
        edits.expansionfiles()
                .update(variant.applicationId, editId, variant.versionCode, type, content)
                .execute()
    }

}
