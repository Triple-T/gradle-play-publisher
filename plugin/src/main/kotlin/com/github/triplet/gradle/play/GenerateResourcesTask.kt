package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.JsonFileFilter
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.LocaleFileFilter
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.findClosestDir
import com.github.triplet.gradle.play.internal.orNull
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

@CacheableTask
open class GenerateResourcesTask : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory
    lateinit var resDir: File

    @TaskAction
    fun generate(inputs: IncrementalTaskInputs) {
        validateAll()
        if (!inputs.isIncremental) project.delete(outputs.files)

        project.copy { spec ->
            inputs.outOfDate {
                it.file.orNull()?.let {
                    spec.from(it)
                    spec.into(it.findClosestDir().findDest())
                }
            }
        }
        inputs.removed { project.delete(it.file.findDest()) }
    }

    private fun validateAll() {
        check(this.inputs.files.all {
            it.listFiles()?.all { file ->
                if (file.isDirectory) {
                    when (file.name) {
                        LISTINGS_PATH -> {
                            file.listFiles()?.all {
                                val isValidLocale = LocaleFileFilter.accept(it)
                                if (!isValidLocale) logger.error("Invalid locale: ${it.name}")
                                isValidLocale
                            } ?: true
                        }
                        PRODUCTS_PATH -> {
                            file.listFiles()?.all {
                                val isValid = JsonFileFilter.accept(it)
                                if (!isValid) {
                                    logger.error("Invalid file type: ${it.name}. Must be JSON.")
                                }
                                isValid
                            } ?: true
                        }
                        else -> {
                            logger.error("Invalid folder: ${it.name}. Must be either " +
                                                 "$LISTINGS_PATH or $PRODUCTS_PATH.")
                            false
                        }
                    }
                } else {
                    val isValid = AppDetail.values().any { file.name == it.fileName }
                    if (!isValid) {
                        val validValues = AppDetail.values().joinToString { "'${it.fileName}'" }
                        logger.error("Invalid app detail: ${it.name}. Must be on of $validValues.")
                    }
                    isValid
                }
            } ?: true
        }) { "Invalid $PLAY_PATH folder, check logs for details." }
    }

    private fun File.findDest() = File(resDir, toRelativeString(findOwner()))

    private fun File.findOwner(): File = inputs.files.filter { startsWith(it) }.single()
}
