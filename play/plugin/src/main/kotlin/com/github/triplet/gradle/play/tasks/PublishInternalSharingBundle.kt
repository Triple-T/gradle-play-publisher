package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.ArtifactExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

internal abstract class PublishInternalSharingBundle @Inject constructor(
        extension: PlayPublisherExtension,
        appId: String
) : PublishTaskBase(extension, appId), ArtifactExtensionOptions {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    internal abstract val bundle: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun publishBundle() {
        project.serviceOf<WorkerExecutor>().noIsolation().submit(BundleUploader::class) {
            paramsForBase(this)

            bundleFile.set(bundle)
            outputDir.set(outputDirectory)
        }
    }

    abstract class BundleUploader : PlayWorkerBase<BundleUploader.Params>() {
        override fun execute() {
            val bundleFile = parameters.bundleFile.get().asFile
            val response = publisher.uploadInternalSharingBundle(bundleFile)

            println("Upload successful: ${response.downloadUrl}")
            parameters.outputDir.get().file("${System.currentTimeMillis()}.json").asFile
                    .writeText(response.json)
        }

        interface Params : PlayPublishingParams {
            val bundleFile: RegularFileProperty
            val outputDir: DirectoryProperty
        }
    }
}
