package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.JsonFileFilter
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.LocaleFileFilter
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import com.github.triplet.gradle.play.internal.climbUpTo
import com.github.triplet.gradle.play.internal.findClosestDir
import com.github.triplet.gradle.play.internal.isChildOf
import com.github.triplet.gradle.play.internal.isDirectChildOf
import com.github.triplet.gradle.play.internal.normalized
import com.github.triplet.gradle.play.internal.nullOrFull
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.playPath
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

@CacheableTask
open class GenerateResources @Inject constructor(
        private val variant: ApplicationVariant
) : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory
    internal val resDir by lazy { File(project.buildDir, "${variant.playPath}/res") }

    private val resSrcDirNames by lazy { variant.sourceSets.map { "src/${it.name}/$PLAY_PATH" } }
    private val resSrcDirs by lazy { resSrcDirNames.map { project.file(it) } }
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val resSrcTree by lazy {
        project.files(resSrcDirNames.map {
            project.fileTree(it).apply { exclude("**/.*") }
        })
    }

    @TaskAction
    fun generate(changes: InputChanges) {
        val files = changes.getFileChanges(resSrcTree).filter { change ->
            if (change.changeType == ChangeType.REMOVED) {
                val file = change.file
                val target = resSrcDirs.singleOrNull { file.startsWith(it) }
                        ?.let { file.toRelativeString(it).nullOrFull() }
                if (target != null) project.delete(File(resDir, target))

                false
            } else {
                true
            }
        }.map { it.file }

        project.serviceOf<WorkerExecutor>().submit(Processor::class) {
            params(Processor.Params(resDir, resSrcDirs, files))
        }
    }

    private class Processor @Inject constructor(private val p: Params) : Runnable {
        override fun run() {
            val defaultLocale = p.resSrcDirs.mapNotNull {
                File(it, AppDetail.DEFAULT_LANGUAGE.fileName).orNull()
                        ?.readText()?.normalized().nullOrFull()
            }.lastOrNull() // Pick the most specialized option available. E.g. `paidProdRelease`

            val files = p.files
                    .filterNot { it.isDirectory }
                    .sortedBy { file ->
                        p.resSrcDirs.indexOf(p.resSrcDirs.singleOrNull { file.startsWith(it) })
                    }
                    .ifEmpty { return }

            val changedDefaults = mutableListOf<File>()
            for (file in files) {
                file.validate()

                defaultLocale.nullOrFull()?.let {
                    if (file.isChildOf(LISTINGS_PATH) && file.isChildOf(it)) {
                        changedDefaults += file
                    }
                }
                file.copy(file.findClosestDir().findDest())
            }

            val writeQueue = mutableListOf<Action<Unit>>()
            for (default in changedDefaults) {
                val listings = default.findDest().climbUpTo(LISTINGS_PATH)!!
                val relativePath = default.invariantSeparatorsPath.split("$defaultLocale/").last()

                listings.listFiles()
                        .filter { it.name != defaultLocale }
                        .map { File(it, relativePath) }
                        .filterNot(File::exists)
                        .filterNot(::hasGraphicCategory)
                        .forEach {
                            writeQueue += Action {
                                default.copy(
                                        File(p.resDir, it.parentFile.toRelativeString(p.resDir)))
                            }
                        }
            }
            writeQueue.forEach { it.execute(Unit) }
        }

        private fun File.validate() {
            fun File.validateLocales() {
                checkNotNull(listFiles()) {
                    "$this must be a folder"
                }.forEach {
                    check(it.isDirectory && LocaleFileFilter.accept(it)) {
                        "Invalid locale: ${it.name}"
                    }
                }
            }

            fun validateListings() {
                val listings = climbUpTo(LISTINGS_PATH) ?: return
                check(listings.isDirectChildOf(PLAY_PATH)) {
                    "Listings ($listings) must be under the '$PLAY_PATH' folder"
                }
                listings.validateLocales()
            }

            fun validateReleaseNotes() {
                val releaseNotes = climbUpTo(RELEASE_NOTES_PATH) ?: return
                check(releaseNotes.isDirectChildOf(PLAY_PATH)) {
                    "Release notes ($releaseNotes) must be under the '$PLAY_PATH' folder"
                }
                releaseNotes.validateLocales()
            }

            fun validateReleaseNames() {
                val releaseNames = climbUpTo(RELEASE_NAMES_PATH) ?: return
                check(releaseNames.isDirectChildOf(PLAY_PATH)) {
                    "Release names ($releaseNames) must be under the '$PLAY_PATH' folder"
                }
            }

            fun validateProducts() {
                val products = climbUpTo(PRODUCTS_PATH) ?: return
                check(products.isDirectChildOf(PLAY_PATH)) {
                    "Products ($products) must be under the '$PLAY_PATH' folder"
                }
                checkNotNull(products.listFiles()) {
                    "$products must be a folder"
                }.forEach {
                    check(JsonFileFilter.accept(it)) { "In-app product files must be JSON." }
                }
            }

            val areRootsValid = isDirectChildOf(PLAY_PATH)
                    || isChildOf(LISTINGS_PATH)
                    || isChildOf(RELEASE_NOTES_PATH)
                    || isChildOf(RELEASE_NAMES_PATH)
                    || isChildOf(PRODUCTS_PATH)
            check(areRootsValid) { "Unknown Play resource file: $this" }

            validateListings()
            validateReleaseNotes()
            validateReleaseNames()
            validateProducts()
        }

        private fun File.copy(dest: File): File = copyTo(File(dest, name), true)

        private fun File.findDest() = File(p.resDir, toRelativeString(findOwner()))

        private fun File.findOwner() = p.resSrcDirs.single { startsWith(it) }

        private fun hasGraphicCategory(file: File): Boolean {
            val graphic = ImageType.values().find { file.isDirectChildOf(it.dirName) }
            return graphic != null && file.climbUpTo(graphic.dirName)?.orNull() != null
        }

        data class Params(
                val resDir: File,
                val resSrcDirs: List<File>,
                val files: List<File>
        ) : Serializable
    }
}
