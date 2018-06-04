package com.github.triplet.gradle.play.internal

import java.io.File
import java.io.FileFilter

internal object ApkFileFilter : FileFilter {
    override fun accept(file: File) = file.extension.toLowerCase() == APK_EXTENSION
}

internal object BundleFileFilter : FileFilter {
    override fun accept(file: File) = file.extension.toLowerCase() == BUNDLE_EXTENSION
}
