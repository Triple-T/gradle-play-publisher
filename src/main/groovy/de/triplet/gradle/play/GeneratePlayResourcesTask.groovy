package de.triplet.gradle.play

import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class GeneratePlayResourcesTask extends DefaultTask {

    File outputFolder

    @TaskAction
    generate() {
        inputs.files.each { dir ->
            if (dir.exists()) {
                FileUtils.copyDirectory(dir, outputFolder)
            }
        }
    }

}
