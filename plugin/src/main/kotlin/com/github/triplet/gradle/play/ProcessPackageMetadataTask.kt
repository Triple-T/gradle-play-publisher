package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import org.gradle.api.tasks.TaskAction

open class ProcessPackageMetadataTask : PlayPublishTaskBase() {
    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun process() {
        progressLogger.start("Updates APK/Bundle metadata for variant ${variant.name}", null)

        if (extension._resolutionStrategy == ResolutionStrategy.AUTO) processVersionCodes()

        progressLogger.completed()
    }

    private fun processVersionCodes() = read { editId ->
        progressLogger.progress("Downloading active version codes")
        val maxVersionCode = tracks().list(variant.applicationId, editId).execute().tracks
                ?.map { it.releases ?: emptyList() }?.flatten()
                ?.map { it.versionCodes ?: emptyList() }?.flatten()
                ?.max() ?: 1

        val outputs = variant.outputs.filterIsInstance<ApkVariantOutput>()
        extension.autoResolutionHandler(AutoResolutionInputs(outputs, maxVersionCode))
    }
}
