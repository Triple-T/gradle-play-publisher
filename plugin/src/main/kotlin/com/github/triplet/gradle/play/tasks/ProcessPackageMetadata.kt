package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.resolutionStrategyOrDefault
import com.github.triplet.gradle.play.tasks.internal.PlayPublishTaskBase
import org.gradle.api.tasks.TaskAction

open class ProcessPackageMetadata : PlayPublishTaskBase() {
    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }

        onlyIf { extension.resolutionStrategyOrDefault == ResolutionStrategy.AUTO }
    }

    @TaskAction
    fun process() {
        progressLogger.start("Updates APK/Bundle metadata for variant ${variant.name}", null)
        processVersionCodes()
        progressLogger.completed()
    }

    private fun processVersionCodes() = read(true) { editId ->
        progressLogger.progress("Downloading active version codes")
        val maxVersionCode = tracks().list(variant.applicationId, editId).execute().tracks
                ?.map { it.releases ?: emptyList() }?.flatten()
                ?.map { it.versionCodes ?: emptyList() }?.flatten()
                ?.max() ?: 1

        val outputs = variant.outputs.filterIsInstance<ApkVariantOutput>()
        val smallestVersionCode = outputs.map { it.versionCode }.min() ?: 1

        val patch = maxVersionCode - smallestVersionCode + 1
        if (patch <= 0) return@read // Nothing to do, outputs are already greater than remote

        for ((i, output) in outputs.withIndex()) {
            output.versionCodeOverride = output.versionCode + patch.toInt() + i
            extension._outputProcessor?.execute(output)
        }
    }
}
