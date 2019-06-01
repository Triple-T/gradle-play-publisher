package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.api.InstallableVariantImpl
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.orNull
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import javax.inject.Inject

open class PublishBundleBase @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PlayPublishArtifactBase(extension, variant), PublishableTrackExtensionOptions {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    protected val bundle: File?
        get() {
            val customDir = extension._artifactDir

            return if (customDir == null) {
                val installable = variant as InstallableVariantImpl
                installable.variantData.scope.artifacts
                        .getFinalProduct<RegularFile>(InternalArtifactType.BUNDLE)
                        .get().asFile.orNull() ?: installable
                        .getFinalArtifact(InternalArtifactType.BUNDLE).files.singleOrNull()
            } else {
                customDir.listFiles().orEmpty().singleOrNull { it.extension == "aab" }.also {
                    if (it == null) println("Warning: no App Bundle found in '$customDir' yet.")
                }
            }
        }
}
