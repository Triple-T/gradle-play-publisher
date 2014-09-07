package de.triplet.gradle.play

import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GeneratePlayResourceTask extends DefaultTask {

    String flavor;

    @OutputDirectory
    File outputFolder;

    @TaskAction
    generate() {

        def inputs = []
        inputs << "main"

        if (!StringUtils.isEmpty(flavor)) {
            inputs << flavor
        }

        inputs.each { input ->
            def flavorDir = new File("${project.getProjectDir().toString()}/src/${input}/play")

            if (flavorDir.exists()) {
                FileUtils.copyDirectory(flavorDir, outputFolder)
            }
        }

    }

}
