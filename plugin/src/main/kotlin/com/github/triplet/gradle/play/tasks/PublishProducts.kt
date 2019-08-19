package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.tasks.internal.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.model.InAppProduct
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class PublishProducts @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishTaskBase(extension, variant) {
    @get:Internal
    internal abstract val resDir: DirectoryProperty
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    protected val productsDir by lazy {
        project.fileTree(resDir.file(PRODUCTS_PATH)).builtBy(resDir).apply {
            include("*.json")
        }
    }

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
                    .createJsonParser(parameters.target.get().inputStream())
                    .parse(InAppProduct::class.java)

            println("Uploading ${product.sku}")
            publisher.inappproducts().update(appId, product.sku, product).execute()
        }

        interface Params : PlayPublishingParams {
            val target: Property<File>
        }
    }
}
