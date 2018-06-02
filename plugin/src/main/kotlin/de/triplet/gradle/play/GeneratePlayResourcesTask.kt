package de.triplet.gradle.play

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class GeneratePlayResourcesTask : DefaultTask() {
    lateinit var outputFolder: File

    @TaskAction
    fun generate() {
        inputs.files
                .filter(File::exists)
                .forEach { it.copyRecursively(outputFolder, true) }
    }
}
