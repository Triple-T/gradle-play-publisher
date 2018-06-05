package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.LocaleFileFilter
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
open class GeneratePlayResourcesTask : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory
    lateinit var resDir: File

    @TaskAction
    fun generate(inputs: IncrementalTaskInputs) {
        check(this.inputs.files.all {
            it.listFiles()?.filter { it.isDirectory }?.all {
                val isValidLocale = LocaleFileFilter.accept(it)
                if (!isValidLocale) logger.error("Invalid locale: ${it.name}")
                isValidLocale
            } ?: true
        }) { "Invalid locale(s), check logs for details." }

        if (!inputs.isIncremental) project.delete(outputs.files)

        inputs.outOfDate {
            it.file.orNull()?.let { input ->
                project.copy {
                    it.from(input)
                    it.into(input.findClosestDir().findDest())
                }
            }
        }
        inputs.removed { project.delete(it.file.findDest()) }
    }

    private fun File.findDest() = File(resDir, toRelativeString(findOwner()))

    private fun File.findOwner(): File = inputs.files.filter { startsWith(it) }.single()
}
