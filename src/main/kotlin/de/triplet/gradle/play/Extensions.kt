package de.triplet.gradle.play

import org.gradle.api.Project
import java.io.File

internal fun String.normalize() = replace(Regex("\\r\\n"), "\n").trim()

internal fun File.readAndTrim(project: Project, maxLength: Int, errorOnSizeLimit: Boolean): String {
    if (exists()) {
        val message = readText().normalize()

        if (message.length > maxLength) {
            if (errorOnSizeLimit) {
                val relativePath = toRelativeString(project.file(RESOURCES_OUTPUT_PATH))
                throw IllegalArgumentException("File '$relativePath' has reached the limit of $maxLength characters")
            }

            return message.substring(0, maxLength)
        }

        return message
    }

    return ""
}

internal fun File.firstLine() = if (exists())
    bufferedReader().lineSequence().first()
else
    ""

