package com.github.triplet.gradle.play.internal

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import java.io.File

internal abstract class ApkSpec {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val artifactDir: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    // loader.load() returns null during configuration-cache store
    // So resolve APKs at task-action time (#1187)
    fun resolve(): List<File> = if (artifactDir.isPresent) {
        sources.files.toList()
    } else {
        builtArtifactsLoader.get().load(sources)?.elements?.map { File(it.outputFile) }
                ?: error("output-metadata.json not found in ${sources.asPath}")
    }
}
