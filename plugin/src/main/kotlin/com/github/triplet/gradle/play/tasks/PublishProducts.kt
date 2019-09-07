package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.model.InAppProduct
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

abstract class PublishProducts @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishTaskBase(extension, variant) {
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal abstract val productsDir: ConfigurableFileCollection

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

    internal abstract class Uploader : PlayWorkerBase<Uploader.Params>() {
        override fun execute() {
            val product = JacksonFactory.getDefaultInstance()
                    .createJsonParser(parameters.target.get().asFile.inputStream())
                    .parse(InAppProduct::class.java)

            println("Uploading ${product.sku}")
            publisher.inappproducts().update(appId, product.sku, product).execute()
        }

        interface Params : PlayPublishingParams {
            val target: RegularFileProperty
        }
    }
}
