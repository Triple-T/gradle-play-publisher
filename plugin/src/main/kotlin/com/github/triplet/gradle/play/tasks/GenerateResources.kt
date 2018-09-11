package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.JsonFileFilter
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.LocaleFileFilter
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import com.github.triplet.gradle.play.internal.climbUpTo
import com.github.triplet.gradle.play.internal.findClosestDir
import com.github.triplet.gradle.play.internal.isChildOf
import com.github.triplet.gradle.play.internal.isDirectChildOf
import com.github.triplet.gradle.play.internal.normalized
import com.github.triplet.gradle.play.internal.nullOrFull
import com.github.triplet.gradle.play.internal.orNull
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

@CacheableTask
open class GenerateResources : DefaultTask() {
    @get:Internal
    internal lateinit var variant: ApplicationVariant

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory
    lateinit var resDir: File

    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val resSrcDirs by lazy {
        variant.sourceSets.map { project.fileTree("src/${it.name}/$PLAY_PATH") }
    }
    private val defaultLocale by lazy {
        resSrcDirs.mapNotNull {
            File(it.dir, AppDetail.DEFAULT_LANGUAGE.fileName).orNull()
                    ?.readText()?.normalized().nullOrFull()
        }.lastOrNull() // Pick the most specialized option available. E.g. `paidProdRelease`
    }

    @TaskAction
    fun generate(inputs: IncrementalTaskInputs) {
        if (!inputs.isIncremental) project.delete(outputs.files)

        val changedDefaults = mutableListOf<File>()

        inputs.outOfDate {
            file.validate()

            defaultLocale?.let {
                if (file.isChildOf(LISTINGS_PATH) && file.isChildOf(it)) changedDefaults += file
            }
            project.copy { from(file).into(file.findClosestDir().findDest()) }
        }
        inputs.removed { project.delete(file.findDest()) }

        for (default in changedDefaults.reversed().distinctBy { it.name }) {
            val listings = default.findDest().climbUpTo(LISTINGS_PATH)!!
            val relativePath = default.invariantSeparatorsPath.split("$defaultLocale/").last()

            listings.listFiles()
                    .filter { it.name != defaultLocale }
                    .map { File(it, relativePath) }
                    .filter { !it.exists() }
                    .forEach {
                        project.copy {
                            from(default).into(File(resDir, it.parentFile.toRelativeString(resDir)))
                        }
                    }
        }
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

        val areRootsValid = climbUpTo(LISTINGS_PATH) != null
                || climbUpTo(RELEASE_NOTES_PATH) != null
                || climbUpTo(PRODUCTS_PATH) != null
                || isDirectChildOf(PLAY_PATH)
        check(areRootsValid) { "Unknown file: $this" }

        validateListings()
        validateReleaseNotes()
        validateProducts()
    }

    private fun File.findDest() = File(resDir, toRelativeString(findOwner()))

    private fun File.findOwner() = resSrcDirs.map { it.dir }.single { startsWith(it) }
}
