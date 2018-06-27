package com.github.triplet.gradle.play.internal

import java.io.File

internal fun File.orNull() = if (exists()) this else null

internal tailrec fun File.findClosestDir(): File {
    check(exists()) { "$this does not exist" }
    return if (isDirectory) this else parentFile.findClosestDir()
}

internal fun File.climbUpTo(parentName: String): File? =
        if (name == parentName) this else parentFile?.climbUpTo(parentName)

internal fun File.isChildOf(parentName: String) = climbUpTo(parentName) != null

internal fun File.isDirectChildOf(parentName: String) = parentFile?.name == parentName

internal fun File.safeMkdirs() = apply {
    check(exists() || mkdirs()) { "Unable to create $this" }
}

internal fun File.safeCreateNewFile() = apply {
    parentFile.safeMkdirs()
    check(exists() || createNewFile()) { "Unable to create $this" }
}

internal fun File.readProcessed(maxLength: Int) =
        readText().normalized().throwOnOverflow(maxLength, this)

internal fun String.normalized() = replace(Regex("\\r\\n"), "\n").trim()

internal fun String?.nullOrFull() = if (isNullOrBlank()) null else this

private fun String.throwOnOverflow(max: Int, file: File): String {
    check(length <= max) { "File '$file' has reached the limit of $max characters." }
    return this
}
