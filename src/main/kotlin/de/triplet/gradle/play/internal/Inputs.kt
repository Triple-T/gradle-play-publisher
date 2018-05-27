package de.triplet.gradle.play.internal

import java.io.File

fun File.orNull() = if (exists()) this else null

fun File.readProcessed(maxLength: Int, error: Boolean) =
        readText().normalized().takeOrThrow(maxLength, error, this)

fun String.normalized() = replace(Regex("\\r\\n"), "\n").trim()

fun String.takeOrThrow(n: Int, error: Boolean, file: File): String {
    val result = take(n)
    if (error) require(result.length == length) {
        "File '$file' has reached the limit of $n characters."
    }
    return result
}
