package com.github.triplet.gradle.play.tasks.internal

import com.android.build.VariantOutput.OutputType
import com.android.build.api.artifact.ArtifactType
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.internal.api.InstallableVariantImpl
import com.android.build.gradle.internal.scope.InternalArtifactType
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import java.io.File

internal fun PublishTaskBase.findBundleFile(): File? {
    val customDir = extension.config.artifactDir

    return if (customDir == null) {
        val installable = variant as InstallableVariantImpl

        // TODO(#708): remove when AGP 3.6 is the minimum
        @Suppress("UNCHECKED_CAST") // We know its type
        fun getFinalArtifactCompat(): Set<File> = try {
            installable.getFinalArtifact(
                    InternalArtifactType.BUNDLE as ArtifactType<FileSystemLocation>
            ).get().files
        } catch (e: NoClassDefFoundError) {
            val enumMethod =
                    InternalArtifactType::class.java.getMethod("valueOf", String::class.java)
            val artifactType = enumMethod.invoke(null, "BUNDLE") as ArtifactType<RegularFile>
            val artifact = installable.javaClass
                    .getMethod("getFinalArtifact", ArtifactType::class.java)
                    .invoke(installable, artifactType)
            artifact.javaClass.getMethod("getFiles").apply {
                isAccessible = true
            }.invoke(artifact) as Set<File>
        }

        getFinalArtifactCompat().singleOrNull()
    } else if (customDir.isFile && customDir.extension == "aab") {
        customDir
    } else {
        val bundles = customDir.listFiles().orEmpty().filter { it.extension == "aab" }
        if (bundles.isEmpty()) {
            logger.warn("Warning: '$customDir' does not yet contain an App Bundle.")
        } else if (bundles.size > 1) {
            logger.warn("Warning: '$customDir' contains multiple App Bundles. Only one is allowed.")
        }
        bundles.singleOrNull()
    }
}

internal fun PublishTaskBase.findApkFiles(allowSplits: Boolean): List<File>? {
    val customDir = extension.config.artifactDir

    return if (customDir == null) {
        var result = variant.outputs.filterIsInstance<ApkVariantOutput>()
        if (!allowSplits) {
            result = result.filter {
                OutputType.valueOf(it.outputType) == OutputType.MAIN || it.filters.isEmpty()
            }
        }
        result.map { it.outputFile }
    } else if (customDir.isFile && customDir.extension == "apk") {
        listOf(customDir)
    } else {
        val apks = customDir.listFiles().orEmpty().filter { it.extension == "apk" }
        if (apks.isEmpty()) {
            logger.warn("Warning: '$customDir' does not yet contain any APKs.")
        }
        apks
    }.ifEmpty { null }
}
