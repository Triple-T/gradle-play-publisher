package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.tasks.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.model.InAppProduct
import org.gradle.api.file.FileType
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
import java.io.Serializable
import javax.inject.Inject

open class PublishProducts @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PlayPublishTaskBase(extension, variant) {
    @get:Internal
    internal lateinit var resDir: File
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    protected val productsDir by lazy {
        project.fileTree(File(resDir, PRODUCTS_PATH)).apply {
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
                    executor.submit(Uploader::class) {
                        paramsForBase(this, Uploader.Params(it.file))
                    }
                }
    }

    private class Uploader @Inject constructor(
            private val p: Params,
            data: PlayPublishingData
    ) : PlayWorkerBase(data) {
        override fun run() {
            val product = JacksonFactory.getDefaultInstance()
                    .createJsonParser(p.target.inputStream())
                    .parse(InAppProduct::class.java)

            println("Uploading ${product.sku}")
            publisher.inappproducts().update(appId, product.sku, product).execute()
        }

        data class Params(val target: File) : Serializable
    }
}
