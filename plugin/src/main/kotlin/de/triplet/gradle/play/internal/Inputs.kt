package de.triplet.gradle.play.internal

import java.io.File

internal fun File.orNull() = if (exists()) this else null

internal fun File.readProcessed(maxLength: Int, error: Boolean) =
        readText().normalized().takeOrThrow(maxLength, error, this)

internal fun String.normalized() = replace(Regex("\\r\\n"), "\n").trim()

internal fun String.takeOrThrow(n: Int, error: Boolean, file: File): String {
    val result = take(n)
    if (error) require(result.length == length) {
        "File '$file' has reached the limit of $n characters."
    }
    return result
}
