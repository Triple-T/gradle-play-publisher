package de.triplet.gradle.play

import com.google.api.client.http.FileContent
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class PlayPublishObbTask extends PlayPublishTask {

    File inputFolder

    @TaskAction
    publishObb() {
        super.publish()

        if (!extension.uploadObbMain && !extension.uploadObbPatch) {
            throw new GradleException("You must select at least to upload the main or patch obb file, see the README")
        }

        if (extension.uploadObbMain) {
            publishOne("main")
        }

        if (extension.uploadObbPatch) {
            publishOne("patch")
        }
    }

    private void publishOne(String type) {
        def obbFile = new File(inputFolder, "obb/${type}")

        if (obbFile.exists()) {
            def newObbFile = new FileContent("application/octet-stream", obbFile)

            edits.expansionfiles()
                    .upload(variant.applicationId, editId, variant.versionCode, type, newObbFile)
                    .execute()

            logger.info("Starting upload of the obb file ({} MB), this may take a while",
                    obbFile.length() / 1024 / 1024)

            edits.commit(variant.applicationId, editId).execute()
        } else {
            throw new GradleException("Please place a file named `${type}` in the `play/obb/` directory")
        }
    }

}
