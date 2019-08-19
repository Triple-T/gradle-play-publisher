package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
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
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
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
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class GenerateResources @Inject constructor(
        private val variant: ApplicationVariant
) : DefaultTask() {
    @get:OutputDirectory
    internal abstract val resDir: DirectoryProperty

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
                if (target != null) project.delete(resDir.file(target))

                false
            } else {
                true
            }
        }.map { it.file }

        project.serviceOf<WorkerExecutor>().noIsolation().submit(Processor::class) {
            resDir.set(this@GenerateResources.resDir.asFile.get())
            resSrcDirs.set(this@GenerateResources.resSrcDirs)
            resFiles.set(files)
        }
    }

    internal abstract class Processor : WorkAction<Processor.Params> {
        override fun execute() {
            val defaultLocale = parameters.resSrcDirs.get().mapNotNull {
                File(it, AppDetail.DEFAULT_LANGUAGE.fileName).orNull()
                        ?.readText()?.normalized().nullOrFull()
            }.lastOrNull() // Pick the most specialized option available. E.g. `paidProdRelease`

            val files = parameters.resFiles.get()
                    .filterNot { it.isDirectory }
                    .sortedBy { file ->
                        val dir = parameters.resSrcDirs.get().singleOrNull {
                            file.startsWith(it)
                        }
                        parameters.resSrcDirs.get().indexOf(dir)
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
                            writeQueue += Action {
                                default.copy(File(
                                        parameters.resDir.get(),
                                        it.parentFile.toRelativeString(parameters.resDir.get())
                                ))
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
                    check(it.isDirectory) {
                        "Files are not allowed under the listings directory: ${it.name}"
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

        private fun File.findDest() = File(parameters.resDir.get(), toRelativeString(findOwner()))

        private fun File.findOwner() = parameters.resSrcDirs.get().single { startsWith(it) }

        private fun hasGraphicCategory(file: File): Boolean {
            val graphic = ImageType.values().find { file.isDirectChildOf(it.dirName) }
            return graphic != null && file.climbUpTo(graphic.dirName)?.orNull() != null
        }

        interface Params : WorkParameters {
            val resDir: Property<File>
            val resSrcDirs: Property<List<File>>
            val resFiles: Property<List<File>>
        }
    }
}
