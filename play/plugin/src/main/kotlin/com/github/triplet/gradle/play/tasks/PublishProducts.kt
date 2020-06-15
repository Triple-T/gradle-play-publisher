package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import com.google.api.client.json.jackson2.JacksonFactory
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

internal abstract class PublishProducts @Inject constructor(
        extension: PlayPublisherExtension,
        appId: String
) : PublishTaskBase(extension, appId) {
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val productsDir: ConfigurableFileCollection

    // Used by Gradle to skip the task if all inputs are empty
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    protected val targetFiles: FileCollection by lazy { productsDir.asFileTree }

    // This directory isn't used, but it's needed for up-to-date checks to work
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:Optional
    @get:OutputDirectory
    protected val outputDir = null

    @TaskAction
    fun publishProducts(changes: InputChanges) {
        val executor = project.serviceOf<WorkerExecutor>()
        changes.getFileChanges(productsDir)
                .filterNot { it.changeType == ChangeType.REMOVED }
                .filter { it.fileType == FileType.FILE }
                .forEach {
                    executor.noIsolation().submit(Uploader::class) {
                        paramsForBase(this)
                        target.set(it.file)
                    }
                }
    }

    abstract class Uploader : PlayWorkerBase<Uploader.Params>() {
        override fun execute() {
            val productFile = parameters.target.get().asFile
            val product = productFile.inputStream().use {
                JacksonFactory.getDefaultInstance().createJsonParser(it).parse(Map::class.java)
            }

            println("Uploading ${product["sku"]}")
            val response = publisher.updateInAppProduct(productFile)
            if (response.needsCreating) publisher.insertInAppProduct(productFile)
        }

        interface Params : PlayPublishingParams {
            val target: RegularFileProperty
        }
    }
}
