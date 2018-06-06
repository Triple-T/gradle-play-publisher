package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import org.gradle.api.tasks.TaskAction

open class ProcessPackageMetadataTask : PlayPublishTaskBase() {
    @TaskAction
    fun process() {
        if (extension._resolutionStrategy != ResolutionStrategy.IGNORE) processVersionCodes()
    }

    private fun processVersionCodes() = read { editId ->
        val maxVersionCode = tracks().list(variant.applicationId, editId).execute().tracks
                ?.map { it.releases ?: emptyList() }?.flatten()
                ?.map { it.versionCodes ?: emptyList() }?.flatten()
                ?.max() ?: 1

        when (extension._resolutionStrategy) {
            ResolutionStrategy.AUTO -> {
                val outputs = variant.outputs.filterIsInstance<ApkVariantOutput>()
                while (outputs.any { it.versionCode <= maxVersionCode }) {
                    for (output in outputs) {
                        val newCode = output.versionCode + 1
                        output.versionCodeOverride = newCode
                        extension.versionNameOverride(newCode)
                                ?.let { output.versionNameOverride = it }
                    }
                }
            }
            ResolutionStrategy.FAIL -> check(variant.versionCode > maxVersionCode) {
                "Version code $maxVersionCode is too low for variant ${variant.name}."
            }
            ResolutionStrategy.IGNORE -> error("Impossible condition")
        }
    }
}
