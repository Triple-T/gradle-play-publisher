package com.github.triplet.gradle.play.tasks.internal

import com.android.build.api.artifact.ArtifactType
import com.android.build.gradle.internal.api.InstallableVariantImpl
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.github.triplet.gradle.play.internal.orNull
import org.gradle.api.file.RegularFile
import java.io.File

fun PlayPublishTaskBase.findBundleFile(): File? {
    val customDir = extension.config.artifactDir

    return if (customDir == null) {
        val installable = variant as InstallableVariantImpl

        // TODO remove when AGP 3.6 is the minimum
        fun getFinalArtifactCompat(): Set<File> = try {
            installable.getFinalArtifact(InternalArtifactType.BUNDLE).get().files
        } catch (e: NoSuchMethodError) {
            val artifact = installable.javaClass
                    .getMethod("getFinalArtifact", ArtifactType::class.java)
                    .invoke(installable, InternalArtifactType.BUNDLE)
            @Suppress("UNCHECKED_CAST")
            artifact.javaClass.getMethod("getFiles").apply {
                isAccessible = true
            }.invoke(artifact) as Set<File>
        }

        installable.variantData.scope.artifacts
                .getFinalProduct<RegularFile>(InternalArtifactType.BUNDLE)
                .get().asFile.orNull() ?: getFinalArtifactCompat().singleOrNull()
    } else if (customDir.isFile && customDir.extension == "aab") {
        customDir
    } else {
        customDir.listFiles().orEmpty().singleOrNull { it.extension == "aab" }.also {
            if (it == null) logger.warn("Warning: no App Bundle found in '$customDir' yet.")
        }
    }
}
