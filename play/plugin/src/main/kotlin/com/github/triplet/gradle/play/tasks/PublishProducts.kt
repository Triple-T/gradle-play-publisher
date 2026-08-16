package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.ProductMetadata
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import com.google.api.client.json.gson.GsonFactory
import com.google.gson.Gson
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
import org.gradle.work.ChangeType
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject
import kotlin.jvm.java

@DisableCachingByDefault
internal abstract class PublishProducts @Inject constructor(
        extension: PlayPublisherExtension,
        private val executor: WorkerExecutor,
) : PublishTaskBase(extension) {
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
        changes.getFileChanges(productsDir)
                .filterNot { it.changeType == ChangeType.REMOVED }
                .filter { it.fileType == FileType.FILE }
                .map {
                    // We should attempt to publish a product if it or its metadata changes.
                    if (it.file.name.endsWith(PRODUCT_METADATA_SUFFIX)) {
                        it.file.parentFile.resolve(
                                it.file.name.dropLast(PRODUCT_METADATA_SUFFIX.length) + ".json")
                    } else {
                        it.file
                    }
                }
                .distinct()
                .forEach {
                    executor.noIsolation().submit(Uploader::class) {
                        paramsForBase(this)
                        target.set(it)
                    }
                }
    }

    abstract class Uploader : PlayWorkerBase<Uploader.Params>() {
        override fun execute() {
            val productFile = parameters.target.get().asFile

            val product = productFile.inputStream().use {
                GsonFactory.getDefaultInstance().createJsonParser(it).parse(Map::class.java)
            }

            val metadata = productFile.parentFile
                    .resolve(productFile.name.dropLast(".json".length) + PRODUCT_METADATA_SUFFIX).inputStream().use {
                        Gson().fromJson(it.reader(), ProductMetadata::class.java)
                    }

            println("Uploading ${product["productId"]}")
            val response = apiService.publisher.updateInAppProduct(productFile, metadata.regionsVersion)
            if (response.needsCreating) {
                println("Creating ${product["productId"]}")
                apiService.publisher.insertInAppProduct(productFile, metadata.regionsVersion)
            }
        }

        interface Params : PlayPublishingParams {
            val target: RegularFileProperty
        }
    }

    companion object {
        /** The file name suffix for product metadata files. */
        const val PRODUCT_METADATA_SUFFIX = ".metadata.json"
    }
}
