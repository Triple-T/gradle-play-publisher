package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.ArtifactExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.findBundleFile
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
import java.io.File
import javax.inject.Inject

internal abstract class PublishInternalSharingBundle @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishTaskBase(extension, variant), ArtifactExtensionOptions {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    protected val bundle: File?
        get() = findBundleFile()

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun publishBundle() {
        val bundle = bundle?.orNull() ?: return
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
