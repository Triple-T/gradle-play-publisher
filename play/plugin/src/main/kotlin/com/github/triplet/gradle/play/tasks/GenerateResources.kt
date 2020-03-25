package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.common.utils.climbUpTo
import com.github.triplet.gradle.common.utils.isChildOf
import com.github.triplet.gradle.common.utils.isDirectChildOf
import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.common.utils.readProcessed
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.common.utils.sibling
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.GRAPHICS_PATH
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NAMES_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileType
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
import java.io.BufferedReader
import java.io.File
import java.util.TreeSet
import javax.inject.Inject

@CacheableTask
internal abstract class GenerateResources : DefaultTask() {
    @get:Internal
    abstract val resSrcDirs: ListProperty<Directory>

    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val resSrcTree: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val resDir: DirectoryProperty

    @TaskAction
    fun generate(changes: InputChanges) {
        val fileChanges = changes.getFileChanges(resSrcTree)
        val validateChanges = fileChanges
                .filterNot { it.changeType == ChangeType.REMOVED }
                .map { it.file }
        val generateChanges = fileChanges
                .filter { it.fileType == FileType.FILE }
                .map { it.changeType to it.file }

        val work = project.serviceOf<WorkerExecutor>().noIsolation()
        if (validateChanges.isNotEmpty()) {
            work.submit(Validator::class) {
                files.set(validateChanges)
                inputDirs.set(resSrcDirs)
            }
        }
        if (generateChanges.isNotEmpty()) {
            work.submit(Generator::class) {
                projectDirectory.set(project.layout.projectDirectory)
                inputDirs.set(resSrcDirs)
                outputDir.set(resDir)
                changedFiles.set(generateChanges)
            }
        }
    }

    abstract class Validator : WorkAction<Validator.Params> {
        override fun execute() {
            for (file in parameters.files.get()) file.validate()
        }

        private fun File.validate() {
            val areRootsValid = name == PLAY_PATH ||
                    isDirectChildOf(PLAY_PATH) ||
                    isChildOf(LISTINGS_PATH) ||
                    isChildOf(RELEASE_NOTES_PATH) ||
                    isChildOf(RELEASE_NAMES_PATH) ||
                    isChildOf(PRODUCTS_PATH)
            check(areRootsValid) { "Unknown Play resource file: $this" }

            val isPlayKeywordReserved = name != PLAY_PATH || parameters.inputDirs.get().any {
                it.asFile == this
            }
            check(isPlayKeywordReserved) {
                "The file name 'play' is illegal: $this"
            }

            check(extension != INDEX_MARKER) { "Resources cannot use the 'index' extension: $this" }
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
            if (isChildOf(LISTINGS_PATH)) validateLocales(listings)
        }

        private fun File.validateReleaseNotes() {
            val releaseNotes = climbUpTo(RELEASE_NOTES_PATH) ?: return
            check(releaseNotes.isDirectChildOf(PLAY_PATH)) {
                "Release notes ($releaseNotes) must be under the '$PLAY_PATH' directory"
            }
            if (isChildOf(RELEASE_NOTES_PATH)) validateLocales(releaseNotes)
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

        interface Params : WorkParameters {
            val files: ListProperty<File>
            val inputDirs: ListProperty<Directory>
        }
    }

    abstract class Generator @Inject constructor(
            private val fileOps: FileSystemOperations
    ) : WorkAction<Generator.Params> {
        private val defaultLocale = findDefaultLocale()
        private val genOrder = newGenOrder()

        override fun execute() {
            // ## Definitions
            //
            // Index: Map<GeneratedFile, SortedSet<ProducerFile>>
            // Reverse index: Map<ProducerFile, SortedSet<GeneratedFile>>
            // Index on disk: combo of the two, looks something like this.
            //   $ cat GeneratedFile.index
            //   -
            //   ProducerFile1
            //   GeneratedFile1
            //   GeneratedFile2
            //   ...
            //   -
            //   ProducerFile2
            //   GeneratedFile1
            //   GeneratedFile2
            //   ...
            //
            // The paths are in unix separators and relative to the project dir.
            //
            // ## Algorithm
            //
            // 1. Collect all locales from src/**/listings/*
            // 2. Collect all previous *.index files from build/**/$outputDir/**
            // 3. Build partial index from input changes
            // 4. Merge both indexes, saving which GeneratedFiles need updating
            // 5. Write new merged index to disk
            // 6. Follow the index to write the changed GeneratedFiles
            //
            // ## Merge algorithm
            //
            // Spec: keep each locale's producers ordered by the $inputDirs. The default locale is
            // on bottom, the actual locale on top.
            //
            // ### ADD
            //
            // 1. If for $defaultLocale: collect all locales and add to reverse index.
            // 2. Update index with each GeneratedFile and its producer.
            //
            // ### INSERT
            //
            // 1. If for $defaultLocale: collect all locales and add to reverse index.
            // 2. For each GeneratedFile:
            //    1. Take all $defaultLocale changes from partial index node and merge them with the
            //       bottom of the node. All $defaultLocale producers should be at the bottom.
            //    2. Take all non-$defaultLocale changes from partial index node and merge them with
            //       the top.
            //
            // ### MODIFY
            //
            // Do nothing. Keeping track of which GeneratedFiles changed is enough.
            //
            // ### DELETE
            //
            // Follow each GeneratedFile the producer produced from the reverse index and remove it
            // from the index.
            //
            // ## Writing the generated files
            //
            // Use the index: the first ProducerFile wins and gets written as the GeneratedFile.

            val (locales, prevIndex, prevReverseIndex) = parseSrcTree()
            val (index, reverseIndex, prunedResources) = buildIndex()

            insertNewLocales(index, reverseIndex, locales)
            mergeExistingReferences(prevIndex, index, reverseIndex)
            pruneOutdatedReferences(prevReverseIndex, index, reverseIndex, prunedResources)
            writeIndex(index, reverseIndex)
            pruneGeneratedResources(prevIndex, prevReverseIndex, index, prunedResources)
            generateResources(index)
        }

        private fun parseSrcTree(): SourceTree {
            val locales = mutableSetOf<String>()
            val index = mutableMapOf<File, MutableSet<File>>()
            val reverseIndex = mutableMapOf<File, MutableSet<File>>()

            for (dir in parameters.inputDirs.get()) {
                dir.asFileTree.visit {
                    if (file.isDirectChildOf(LISTINGS_PATH) && name != defaultLocale) {
                        locales += name
                    }
                }
            }
            parameters.outputDir.get().asFileTree.visit {
                if (file.extension == INDEX_MARKER) {
                    open().bufferedReader().use { reader ->
                        reader.readIndex(index, reverseIndex)
                    }
                }
            }

            return SourceTree(locales, index, reverseIndex)
        }

        private fun BufferedReader.readIndex(
                index: MutableMap<File, MutableSet<File>>,
                reverseIndex: MutableMap<File, MutableSet<File>>
        ) {
            var line: String?
            lateinit var producer: File

            while (true) {
                line = readLine()
                if (line == null) break

                if (line == "-") {
                    producer = parameters.projectDirectory.get().file(readLine()).asFile
                    continue
                }

                val generated = parameters.projectDirectory.get().file(line).asFile
                safeAddValue(index, reverseIndex, generated, producer)
            }
        }

        private fun buildIndex(): Index {
            val index = mutableMapOf<File, MutableSet<File>>()
            val reverseIndex = mutableMapOf<File, MutableSet<File>>()
            val prunedResources = mutableSetOf<File>()

            for ((type, producer) in parameters.changedFiles.get()) {
                if (type == ChangeType.REMOVED) prunedResources += producer
                safeAddValue(index, reverseIndex, producer.findDest(), producer)
            }

            return Index(index, reverseIndex, prunedResources)
        }

        private fun insertNewLocales(
                index: MutableMap<File, MutableSet<File>>,
                reverseIndex: MutableMap<File, MutableSet<File>>,
                locales: Set<String>
        ) {
            for (producer in reverseIndex.keys.toSet()) {
                if (!producer.isDefaultResource()) continue

                val listings = producer.climbUpTo(LISTINGS_PATH)!!
                val pathFromDefault = producer.toRelativeString(File(listings, defaultLocale!!))
                val destListings = listings.findDest()

                for (locale in locales) {
                    val genLocale = File(File(destListings, locale), pathFromDefault)
                    safeAddValue(index, reverseIndex, genLocale, producer)
                }
            }
        }

        private fun mergeExistingReferences(
                prevIndex: Map<File, Set<File>>,
                index: MutableMap<File, MutableSet<File>>,
                reverseIndex: MutableMap<File, MutableSet<File>>
        ) {
            for (generated in index.keys.toSet()) {
                val prevProducers = prevIndex[generated].orEmpty()
                for (prevProducer in prevProducers) {
                    safeAddValue(index, reverseIndex, generated, prevProducer)
                }
            }
        }

        private fun pruneOutdatedReferences(
                prevReverseIndex: Map<File, Set<File>>,
                index: MutableMap<File, MutableSet<File>>,
                reverseIndex: MutableMap<File, MutableSet<File>>,
                prunedResources: Set<File>
        ) {
            for (prevProducer in prunedResources) {
                val prevGens = prevReverseIndex.getValue(prevProducer)

                reverseIndex -= prevProducer
                for (prevGenerated in prevGens) {
                    val producers = index.getValue(prevGenerated)
                    producers -= prevProducer
                    if (producers.isEmpty()) index -= prevGenerated
                }
            }
        }

        private fun writeIndex(
                index: Map<File, Set<File>>,
                reverseIndex: Map<File, Set<File>>
        ) {
            val projectDir = parameters.projectDirectory.get().asFile
            for ((generated, producers) in index) {
                val builder = StringBuilder()
                for (producer in producers) {
                    builder.apply {
                        append("-").append("\n")
                        val pathFromRootToProducer =
                                producer.relativeTo(projectDir).invariantSeparatorsPath
                        append(pathFromRootToProducer).append("\n")
                    }

                    for (reverseGenerated in reverseIndex.getValue(producer)) {
                        val pathFromRootToGenerated =
                                reverseGenerated.relativeTo(projectDir).invariantSeparatorsPath
                        builder.append(pathFromRootToGenerated).append("\n")
                    }
                }
                generated.marked(INDEX_MARKER).safeCreateNewFile().writeText(builder.toString())
            }
        }

        private fun pruneGeneratedResources(
                prevIndex: Map<File, Set<File>>,
                prevReverseIndex: Map<File, Set<File>>,
                index: Map<File, Set<File>>,
                prunedResources: Set<File>
        ) {
            for (producer in prunedResources) {
                val prevGens = prevReverseIndex.getValue(producer)
                for (prevGenerated in prevGens) {
                    val prevProducers = prevIndex.getValue(prevGenerated)
                    if (prevProducers.first() == producer && index[prevGenerated] == null) {
                        fileOps.delete {
                            delete(prevGenerated, prevGenerated.marked(INDEX_MARKER))
                        }
                    }
                }
            }
        }

        private fun generateResources(index: Map<File, Set<File>>) {
            for ((generated, producers) in index) {
                fileOps.copy {
                    from(producers.first())
                    into(generated.parentFile)
                }
            }
        }

        private fun safeAddValue(
                index: MutableMap<File, MutableSet<File>>,
                reverseIndex: MutableMap<File, MutableSet<File>>,
                generated: File,
                producer: File
        ) {
            index.safeAddValue(generated, producer)
            reverseIndex.safeAddValue(producer, generated)
        }

        private fun MutableMap<File, MutableSet<File>>.safeAddValue(key: File, value: File) {
            val store = get(key) ?: TreeSet(genOrder)
            store += value
            put(key, store)
        }

        private fun File.isDefaultResource(): Boolean {
            val defaultLocale = defaultLocale
            return defaultLocale != null &&
                    isChildOf(LISTINGS_PATH) &&
                    isDirectChildOf(defaultLocale)
        }

        private fun File.findDest(): File {
            val default = File(parameters.outputDir.get().asFile, toRelativeString(findOwner()))
            val isTopLevelGraphic = default.isDirectChildOf(GRAPHICS_PATH) &&
                    ImageType.values().any { default.nameWithoutExtension == it.dirName }

            return if (isTopLevelGraphic) {
                default.sibling(default.nameWithoutExtension + "/" + default.name)
            } else {
                default
            }
        }

        private fun File.findOwner() =
                parameters.inputDirs.get().single { startsWith(it.asFile) }.asFile

        private fun newGenOrder() = compareBy<File> { f ->
            f.isDefaultResource()
        }.thenByDescending { f ->
            val flavor = checkNotNull(f.climbUpTo(PLAY_PATH)?.parentFile?.name) {
                "File not a play resource: $f"
            }
            parameters.inputDirs.get().indexOfFirst {
                it.asFile.parentFile.name == flavor
            }
        }.thenBy { f ->
            f.path
        }

        private fun findDefaultLocale() = parameters.inputDirs.get().mapNotNull {
            it.file(AppDetail.DEFAULT_LANGUAGE.fileName).asFile.orNull()?.readProcessed()
        }.lastOrNull() // Pick the most specialized option available. E.g. `paidProdRelease`

        data class SourceTree(
                val locales: Set<String>,
                val prevIndex: Map<File, Set<File>>,
                val prevReverseIndex: Map<File, Set<File>>
        )

        data class Index(
                val index: MutableMap<File, MutableSet<File>>,
                val reverseIndex: MutableMap<File, MutableSet<File>>,
                val prunedResources: Set<File>
        )

        interface Params : WorkParameters {
            val projectDirectory: DirectoryProperty
            val outputDir: DirectoryProperty
            val inputDirs: ListProperty<Directory>
            val changedFiles: ListProperty<Pair<ChangeType, File>>
        }
    }

    private companion object {
        const val INDEX_MARKER = "index"
    }
}
