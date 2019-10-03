package com.github.triplet.gradle.play.internal

import java.io.File

internal fun File.orNull() = takeIf { exists() }

internal fun File.marked(marker: String) = File(parentFile, "$nameWithoutExtension.$marker")

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

internal fun File.readProcessed() = readText().normalized()

internal fun String.normalized() = replace(Regex("\\r\\n"), "\n").trim()

internal fun String?.nullOrFull() = takeUnless { isNullOrBlank() }
