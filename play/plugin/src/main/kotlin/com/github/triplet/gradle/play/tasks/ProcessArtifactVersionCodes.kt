package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.PublishEditTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.EditWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

internal abstract class ProcessArtifactVersionCodes @Inject constructor(
        extension: PlayPublisherExtension,
        appId: String
) : PublishEditTaskBase(extension, appId) {
    @get:Input
    internal abstract val versionCodes: ListProperty<Int>

    @get:OutputFile
    internal abstract val playVersionCodes: RegularFileProperty

    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun process() {
        project.serviceOf<WorkerExecutor>().noIsolation().submit(VersionCoder::class) {
            paramsForBase(this)
            defaultVersionCodes.set(versionCodes)
            nextAvailableVersionCodes.set(playVersionCodes)
        }
    }

    abstract class VersionCoder : EditWorkerBase<VersionCoder.Params>() {
        override fun execute() {
            val maxVersionCode = edits.findMaxAppVersionCode()

            val smallestVersionCode = parameters.defaultVersionCodes.get().min() ?: 1

            val outputLines = StringBuilder()
            val patch = maxVersionCode - smallestVersionCode + 1
            for ((i, default) in parameters.defaultVersionCodes.get().withIndex()) {
                if (patch > 0) outputLines.append(default + patch.toInt() + i).append("\n")
            }

            parameters.nextAvailableVersionCodes.get().asFile.safeCreateNewFile()
                    .writeText(outputLines.toString())
        }

        interface Params : EditPublishingParams {
            val defaultVersionCodes: ListProperty<Int>
            val nextAvailableVersionCodes: RegularFileProperty
        }
    }
}
