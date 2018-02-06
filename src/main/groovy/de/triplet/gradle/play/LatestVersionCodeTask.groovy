package de.triplet.gradle.play

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.tasks.TaskAction

class LatestVersionCodeTask extends PlayPublishTask {

    @TaskAction
    updateVersion() {
        publish()
        def currentVersionCode = requestVersionCode(variant)
        overrideVersionCode(currentVersionCode  + 1)
    }

    private void overrideVersionCode(int newVersionCode) {
        variant.outputs.all { output ->
            output.setVersionCodeOverride(newVersionCode)
            logger.info("Set VersionCode to ${newVersionCode} for ${output.name} file: ${output.outputFile}")
        }
    }

    private int requestVersionCode(ApplicationVariant variant) {
        logger.info("Request latest VersionCode for variant '${variant.name}'")
        def apksResponse = edits.apks()
                .list(variant.applicationId, editId)
                .execute()

        def apk = apksResponse.apks.last()
        def currentVersionCode = apk.versionCode
        logger.info("Found latest VersionCode for variant '${variant.name}': ${currentVersionCode}")
        currentVersionCode
    }
}
