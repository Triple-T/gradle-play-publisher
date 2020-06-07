package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.androidpublisher.EditManager
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.config
import com.github.triplet.gradle.play.internal.serviceAccountCredentialsOrDefault
import com.github.triplet.gradle.play.tasks.internal.PublishEditTaskBase
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

internal abstract class ProcessArtifactMetadata @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishEditTaskBase(extension, variant) {
    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun process() {
        val publisher = PlayPublisher(
                extension.config.serviceAccountCredentialsOrDefault,
                extension.config.serviceAccountEmail,
                variant.applicationId
        )
        val edits = EditManager(publisher, editId)
        val maxVersionCode = edits.findMaxAppVersionCode()

        val outputs = variant.outputs.filterIsInstance<ApkVariantOutput>()
        val smallestVersionCode = outputs.map { it.versionCode }.min() ?: 1

        val patch = maxVersionCode - smallestVersionCode + 1
        for ((i, output) in outputs.withIndex()) {
            if (patch > 0) output.versionCodeOverride = output.versionCode + patch.toInt() + i
            extension.config.outputProcessor?.execute(output)
        }
    }
}
