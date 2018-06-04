package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import org.gradle.api.tasks.TaskAction

open class ProcessPackageMetadataTask : PlayPublishTaskBase() {
    @TaskAction
    fun processVersionCodes() = read { editId ->
        val maxVersionCode = bundles().list(variant.applicationId, editId).execute().bundles
                ?.map { it.versionCode }
                ?.max() ?: apks().list(variant.applicationId, editId).execute().apks
                ?.map { it.versionCode }
                ?.max() ?: 1

        when (extension._resolutionStrategy) {
            ResolutionStrategy.AUTO -> variant.outputs.filterIsInstance<ApkVariantOutput>().forEach { output ->
                val newCode = maxVersionCode + 1
                output.versionCodeOverride = newCode
                extension.versionNameOverride(newCode)?.let { output.versionNameOverride = it }
            }
            ResolutionStrategy.FAIL -> check(variant.versionCode > maxVersionCode) {
                "Version code $maxVersionCode is too low for variant ${variant.name}."
            }
            ResolutionStrategy.IGNORE -> if (variant.versionCode <= maxVersionCode) logger.warn(
                    "Version code $maxVersionCode may be too low for variant ${variant.name}.")
        }
    }
}
