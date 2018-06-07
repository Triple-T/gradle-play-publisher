package com.github.triplet.gradle.play.internal

import java.io.File

internal fun File.orNull() = if (exists()) this else null

internal tailrec fun File.findClosestDir(): File {
    check(exists()) { "$this does not exist" }
    return if (isDirectory) this else parentFile.findClosestDir()
}

internal fun File.climbUpTo(parentName: String): File? =
        if (name == parentName) this else parentFile?.climbUpTo(parentName)

internal fun File.flattened(): List<File> =
        listFiles()?.map { it.flattened() }?.flatten() ?: listOf(this)

internal fun File.readProcessed(maxLength: Int, error: Boolean) =
        readText().normalized().takeOrThrow(maxLength, error, this)

internal fun File.isDirectChildOf(parentName: String) = parentFile?.name == parentName

internal fun File.safeCreateNewFile() = apply {
    check(parentFile.exists() || parentFile.mkdirs()) { "Unable to create $parentFile" }
    check(exists() || createNewFile()) { "Unable to create $this" }
}

internal fun String.normalized() = replace(Regex("\\r\\n"), "\n").trim()

internal fun String?.nullOrFull() = if (isNullOrBlank()) null else this

internal fun String.takeOrThrow(n: Int, error: Boolean, file: File): String {
    val result = take(n)
    if (error) require(result.length == length) {
        "File '$file' has reached the limit of $n characters."
    }
    return result
}
