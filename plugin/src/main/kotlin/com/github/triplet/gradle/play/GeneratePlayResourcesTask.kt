package com.github.triplet.gradle.play

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
open class GeneratePlayResourcesTask : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory
    lateinit var resDir: File

    @TaskAction
    fun generate() {
        resDir.deleteRecursively()
        inputs.files
                .filter(File::exists)
                .forEach { it.copyRecursively(resDir, true) }
    }
}
