package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.isChildOf
import com.github.triplet.gradle.play.internal.playPath
import com.google.api.services.androidpublisher.model.InAppProduct
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

open class PublishProductsTask : PlayPublishTaskBase() {
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    lateinit var resDir: File
    @Suppress("MemberVisibilityCanBePrivate") // Needed for Gradle caching to work correctly
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputFile
    val outputFile by lazy { File(project.buildDir, "${variant.playPath}/products-cache-key") }

    @TaskAction
    fun publishListing(inputs: IncrementalTaskInputs) {
        if (!inputs.isIncremental) project.delete(outputs.files)

        val changedProducts = mutableSetOf<File>()

        fun File.process() {
            if (invalidatesProduct()) changedProducts += this
        }

        inputs.outOfDate { it.file.process() }
        inputs.removed { it.file.process() }

        publisher.inappproducts().apply {
            changedProducts.map {
                gson.fromJson(it.readText(), InAppProduct::class.java)
            }.forEach {
                logger.error(it.toString())
//                update(variant.applicationId, it.sku, it).execute()
            }

            outputFile.writeText(hashCode().toString())
        }
    }

    private fun File.invalidatesProduct() = isChildOf(PRODUCTS_PATH)
}
