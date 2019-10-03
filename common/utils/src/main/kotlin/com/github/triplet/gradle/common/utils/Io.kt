package com.github.triplet.gradle.common.utils

import java.io.File

fun File.orNull() = takeIf { exists() }

fun File.marked(marker: String) = File(parentFile, "$nameWithoutExtension.$marker")

fun File.climbUpTo(parentName: String): File? =
        if (name == parentName) this else parentFile?.climbUpTo(parentName)

fun File.isChildOf(parentName: String) = climbUpTo(parentName) != null

fun File.isDirectChildOf(parentName: String) = parentFile?.name == parentName

fun File.safeMkdirs() = apply {
    check(exists() || mkdirs()) { "Unable to create $this" }
}

fun File.safeCreateNewFile() = apply {
    parentFile.safeMkdirs()
    check(exists() || createNewFile()) { "Unable to create $this" }
}

fun File.readProcessed() = readText().normalized()

fun String.normalized() = replace(Regex("\\r\\n"), "\n").trim()

fun String?.nullOrFull() = takeUnless { isNullOrBlank() }
