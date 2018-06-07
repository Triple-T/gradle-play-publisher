package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.internal.LocaleFileFilter
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.climbUpTo
import com.github.triplet.gradle.play.internal.findClosestDir
import com.github.triplet.gradle.play.internal.flattened
import com.github.triplet.gradle.play.internal.orNull
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

@CacheableTask
open class GenerateResourcesTask : DefaultTask() {
    @get:Internal
    lateinit var variant: ApplicationVariant

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val resSrcDirs by lazy { variant.sourceSets.map { project.file("src/${it.name}/$PLAY_PATH") } }
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory
    lateinit var resDir: File

    @TaskAction
    fun generate(inputs: IncrementalTaskInputs) {
        if (!inputs.isIncremental) {
            validate()
            project.delete(outputs.files)
        }

        project.copy { spec ->
            inputs.outOfDate {
                it.file.orNull()?.let {
                    spec.from(it)
                    spec.into(it.findClosestDir().findDest())
                }
            }
        }
        inputs.removed { project.delete(it.file.findDest()) }
    }

    private fun validate() {
        check(resSrcDirs.all {
            it.listFiles()?.filter { it.isDirectory }?.all {
                val isValidLocale = LocaleFileFilter.accept(it)
                if (!isValidLocale) logger.error("Invalid locale: ${it.name}")
                isValidLocale
            } ?: true
        }) { "Invalid locale(s), check logs for details." }

        val flavors = variant.baseName.split("-").run { take(size - 1) }
        val flavorTree = resSrcDirs
                .filter { flavors.contains(it.parentFile?.name) }
                .map { it.flattened() }
                .flatten()
                .map { File(it.path.removePrefix(it.climbUpTo(PLAY_PATH)!!.path)) }

        val files = mutableSetOf<File>()
        for (file in flavorTree) check(files.add(file)) {
            "File '$file' is duplicated in $flavors with identical priority."
        }
    }

    private fun File.findDest() = File(resDir, toRelativeString(findOwner()))

    private fun File.findOwner(): File = resSrcDirs.single { startsWith(it) }
}
