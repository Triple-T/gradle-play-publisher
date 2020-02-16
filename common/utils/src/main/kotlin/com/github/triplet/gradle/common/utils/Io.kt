package com.github.triplet.gradle.common.utils

import org.gradle.kotlin.dsl.support.normaliseLineSeparators
import java.io.File

/** @return this file if it exists, null otherwise */
fun File.orNull(): File? = takeIf { exists() }

/** @return a new file on the same level as this file named [name]. */
fun File.sibling(name: String): File = File(parentFile, name)

/** @return a new file marked with the [marker] for bookkeeping of the original file. */
fun File.marked(marker: String): File = sibling("$nameWithoutExtension.$marker")

/**
 * Returns a new parent file named [parentName] if found, null otherwise.
 *
 * Note: file lookups are inclusive. That is, if the current file is named [parentName], it is
 * returned without looking for true parents.
 *
 * @see [isChildOf], [isDirectChildOf]
 */
fun File.climbUpTo(parentName: String): File? =
        if (name == parentName) this else parentFile?.climbUpTo(parentName)

/**
 * @return true if this file is a child of [parentName], false otherwise
 * @see [climbUpTo], [isDirectChildOf]
 */
fun File.isChildOf(parentName: String): Boolean = parentFile?.climbUpTo(parentName) != null

/**
 * @return true if this file's direct parent (1 level up) is [parentName], false otherwise
 * @see [climbUpTo], [isChildOf]
 */
fun File.isDirectChildOf(parentName: String): Boolean = parentFile?.name == parentName

/** @return this directory after ensuring that it either already exists or was created */
fun File.safeMkdirs(): File = apply {
    val create = { exists() || mkdirs() }
    check(create() || create()) { "Unable to create $this" }
}

/** @return this file after ensuring that it either already exists or was created */
fun File.safeCreateNewFile(): File = apply {
    parentFile.safeMkdirs()

    val create = { exists() || createNewFile() }
    check(create() || create()) { "Unable to create $this" }
}

/**
 * Returns the contents of this file trimmed and with line separators normalized, or null if the
 * file is empty.
 */
fun File.readProcessed(): String? = readText().normaliseLineSeparators().trim().nullOrFull()

/** @return this string if it contains content, null otherwise */
fun String?.nullOrFull(): String? = takeUnless { isNullOrBlank() }
