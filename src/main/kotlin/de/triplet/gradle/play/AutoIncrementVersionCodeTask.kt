package de.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import org.gradle.api.tasks.TaskAction

open class AutoIncrementVersionCodeTask : PlayPublishTask() {

    @TaskAction
    fun autoIncrement() {
        publish()

        val versionCode = requestLatestPlayVersionCode()
        overrideVersionCode(versionCode + 1)
    }

    private fun overrideVersionCode(versionCode: Int) {
        variant.outputs.filterIsInstance<ApkVariantOutput>().map { it.versionCodeOverride = versionCode }
    }

    private fun requestLatestPlayVersionCode(): Int {
        logger.info("Request latest VersionCode for application '${variant.applicationId}'")
        val apksListResponse = edits.apks().list(variant.applicationId, editId).execute()
        val latestApk = apksListResponse.apks.last()
        val latestVersionCode = latestApk.versionCode
        logger.info("Found this play version code for application '${variant.applicationId}': $latestVersionCode")
        return latestVersionCode ?: 0
    }
}