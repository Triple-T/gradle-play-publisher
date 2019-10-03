package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.common.utils.climbUpTo
import com.github.triplet.gradle.common.utils.isChildOf
import com.github.triplet.gradle.common.utils.isDirectChildOf
import com.github.triplet.gradle.common.utils.normalized
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File

@CacheableTask
abstract class GenerateResources : DefaultTask() {
    @get:Internal
    internal abstract val resSrcDirs: ListProperty<Directory>
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal abstract val resSrcTree: ConfigurableFileCollection

    @get:OutputDirectory
    internal abstract val resDir: DirectoryProperty

    @TaskAction
    fun generate(changes: InputChanges) {
        val files = changes.getFileChanges(resSrcTree).filter { change ->
            if (change.changeType == ChangeType.REMOVED) {
                val file = change.file
                val target = resSrcDirs.get()
                        .map { it.asFile }
                        .singleOrNull { file.startsWith(it) }
                        ?.let { file.toRelativeString(it).nullOrFull() }
                if (target != null) project.delete(resDir.file(target))

                false
            } else {
                true
            }
        }.map { it.file }

        project.serviceOf<WorkerExecutor>().noIsolation().submit(Processor::class) {
            outputDir.set(resDir)
            inputDirs.set(resSrcDirs)
            resources.set(files)
        }
    }

    internal abstract class Processor : WorkAction<Processor.Params> {
        override fun execute() {
            val defaultLocale = parameters.inputDirs.get().mapNotNull {
                it.file(AppDetail.DEFAULT_LANGUAGE.fileName).asFile.orNull()
                        ?.readText()?.normalized().nullOrFull()
            }.lastOrNull() // Pick the most specialized option available. E.g. `paidProdRelease`

            val files = parameters.resources.get()
                    .filterNot { it.isDirectory }
                    .sortedBy { file ->
                        val dir = parameters.inputDirs.get().singleOrNull {
                            file.startsWith(it.asFile)
                        }
                        parameters.inputDirs.get().indexOf(dir)
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
                        .orEmpty()
                        .filter { it.name != defaultLocale }
                        .map { File(it, relativePath) }
                        .filterNot(File::exists)
                        .filterNot(::hasGraphicCategory)
                        .forEach {
                            val destName = it.parentFile.toRelativeString(
                                    parameters.outputDir.get().asFile)
                            val dest = File(parameters.outputDir.get().asFile, destName)

                            writeQueue += Action { default.copy(dest) }
                        }
            }
            writeQueue.forEach { it.execute(Unit) }
        }

        private fun File.validate() {
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

        private fun File.validateListings() {
            val listings = climbUpTo(LISTINGS_PATH) ?: return
            check(listings.isDirectChildOf(PLAY_PATH)) {
                "Listings ($listings) must be under the '$PLAY_PATH' directory"
            }
            validateLocales(listings)
        }

        private fun File.validateReleaseNotes() {
            val releaseNotes = climbUpTo(RELEASE_NOTES_PATH) ?: return
            check(releaseNotes.isDirectChildOf(PLAY_PATH)) {
                "Release notes ($releaseNotes) must be under the '$PLAY_PATH' directory"
            }
            validateLocales(releaseNotes)
        }

        private fun File.validateReleaseNames() {
            val releaseNames = climbUpTo(RELEASE_NAMES_PATH) ?: return
            check(releaseNames.isDirectChildOf(PLAY_PATH)) {
                "Release names ($releaseNames) must be under the '$PLAY_PATH' directory"
            }
            check(releaseNames.isDirectory) {
                "$releaseNames must be a directory"
            }
        }

        private fun File.validateProducts() {
            val products = climbUpTo(PRODUCTS_PATH) ?: return
            check(products.isDirectChildOf(PLAY_PATH)) {
                "Products ($products) must be under the '$PLAY_PATH' directory"
            }
            check(products.isDirectory) {
                "$products must be a directory"
            }
        }

        private fun File.validateLocales(category: File) {
            // Locales should be a child directory of the category
            var locale = this
            while (locale.parentFile != category) {
                locale = locale.parentFile
            }

            check(locale.isDirectory) {
                "Files are not allowed under the ${category.name} directory: ${locale.name}"
            }
        }

        private fun File.copy(dest: File): File = copyTo(File(dest, name), true)

        private tailrec fun File.findClosestDir(): File {
            check(exists()) { "$this does not exist" }
            return if (isDirectory) this else parentFile.findClosestDir()
        }

        private fun File.findDest() =
                File(parameters.outputDir.get().asFile, toRelativeString(findOwner()))

        private fun File.findOwner() =
                parameters.inputDirs.get().single { startsWith(it.asFile) }.asFile

        private fun hasGraphicCategory(file: File): Boolean {
            val graphic = ImageType.values().find { file.isDirectChildOf(it.dirName) }
            return graphic != null && file.climbUpTo(graphic.dirName)?.orNull() != null
        }

        interface Params : WorkParameters {
            val outputDir: DirectoryProperty
            val inputDirs: ListProperty<Directory>
            val resources: ListProperty<File>
        }
    }
}
