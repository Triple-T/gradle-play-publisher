package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.ArtifactExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.CliOptionsImpl
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.copy
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

internal abstract class PublishInternalSharingBundle @Inject constructor(
        extension: PlayPublisherExtension,
        executionDir: Directory,
        private val executor: WorkerExecutor,
) : PublishTaskBase(extension),
        ArtifactExtensionOptions by CliOptionsImpl(extension, executionDir) {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    internal abstract val bundles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun publishBundle() {
        executor.noIsolation().submit(Processor::class) {
            paramsForBase(this)

            bundleFiles.set(bundles)
            outputDir.set(outputDirectory)
        }
    }

    abstract class Processor @Inject constructor(
            private val executor: WorkerExecutor,
    ) : PlayWorkerBase<Processor.Params>() {
        override fun execute() {
            for (bundle in parameters.bundleFiles.get()) {
                executor.noIsolation().submit(BundleUploader::class) {
                    parameters.copy(this)

                    bundleFile.set(bundle)
                    outputDir.set(parameters.outputDir)
                }
            }
        }

        interface Params : PlayPublishingParams {
            val bundleFiles: ListProperty<File>
            val outputDir: DirectoryProperty
        }
    }

    abstract class BundleUploader : PlayWorkerBase<BundleUploader.Params>() {
        override fun execute() {
            val bundleFile = parameters.bundleFile.get().asFile
            val response = apiService.publisher.uploadInternalSharingBundle(bundleFile)

            println("Upload successful: ${response.downloadUrl}")
            parameters.outputDir.get().file("${bundleFile.nameWithoutExtension}.json").asFile
                    .writeText(response.json)
        }

        interface Params : PlayPublishingParams {
            val bundleFile: RegularFileProperty
            val outputDir: DirectoryProperty
        }
    }
}
