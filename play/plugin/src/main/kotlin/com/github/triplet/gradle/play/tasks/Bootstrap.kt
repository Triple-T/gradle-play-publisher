package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.GppListing
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.common.utils.safeRenameTo
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.GRAPHICS_PATH
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import com.github.triplet.gradle.play.tasks.internal.BootstrapOptions
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.EditWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.copy
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.net.URL
import javax.inject.Inject

internal abstract class Bootstrap @Inject constructor(
        extension: PlayPublisherExtension,
        optionsHolder: BootstrapOptions.Holder,
        private val fileOps: FileSystemOperations,
        private val executor: WorkerExecutor,
) : PublishTaskBase(extension), BootstrapOptions by optionsHolder {
    @get:OutputDirectory
    abstract val srcDir: DirectoryProperty

    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun bootstrap() {
        fileOps.delete { delete(srcDir) }

        if (downloadAppDetails) bootstrapAppDetails(executor)
        if (downloadListings) bootstrapListings(executor)
        if (downloadReleaseNotes) bootstrapReleaseNotes(executor)
        if (downloadProducts) bootstrapProducts(executor)
    }

    private fun bootstrapAppDetails(executor: WorkerExecutor) {
        executor.noIsolation().submit(DetailsDownloader::class) {
            paramsForBase(this)
            dir.set(srcDir)
        }
    }

    private fun bootstrapListings(executor: WorkerExecutor) {
        executor.noIsolation().submit(ListingsDownloader::class) {
            paramsForBase(this)
            dir.set(srcDir.dir(LISTINGS_PATH))
        }
    }

    private fun bootstrapReleaseNotes(executor: WorkerExecutor) {
        executor.noIsolation().submit(ReleaseNotesDownloader::class) {
            paramsForBase(this)
            dir.set(srcDir.dir(RELEASE_NOTES_PATH))
        }
    }

    private fun bootstrapProducts(executor: WorkerExecutor) {
        executor.noIsolation().submit(ProductsDownloader::class) {
            paramsForBase(this)
            dir.set(srcDir.dir(PRODUCTS_PATH))
        }
    }

    abstract class DetailsDownloader : EditWorkerBase<DetailsDownloader.Params>() {
        override fun execute() {
            println("Downloading app details")
            val details = apiService.edits.getAppDetails()

            details.defaultLocale.nullOrFull()?.write(AppDetail.DEFAULT_LANGUAGE)
            details.contactEmail.nullOrFull()?.write(AppDetail.CONTACT_EMAIL)
            details.contactPhone.nullOrFull()?.write(AppDetail.CONTACT_PHONE)
            details.contactWebsite.nullOrFull()?.write(AppDetail.CONTACT_WEBSITE)
        }

        private fun String.write(detail: AppDetail) =
                parameters.dir.get().file(detail.fileName).write(this)

        interface Params : EditPublishingParams {
            val dir: DirectoryProperty
        }
    }

    abstract class ListingsDownloader @Inject constructor(
            private val executor: WorkerExecutor,
    ) : EditWorkerBase<ListingsDownloader.Params>() {
        override fun execute() {
            println("Downloading listings")
            val listings = apiService.edits.getListings()

            for (listing in listings) {
                val rootDir = parameters.dir.get().dir(listing.locale)

                listing.writeMetadata(rootDir)
                listing.fetchImages(rootDir)
            }
        }

        private fun GppListing.writeMetadata(rootDir: Directory) {
            fun String.write(detail: ListingDetail) = rootDir.file(detail.fileName).write(this)

            println("Downloading $locale listing")
            fullDescription.nullOrFull()?.write(ListingDetail.FULL_DESCRIPTION)
            shortDescription.nullOrFull()?.write(ListingDetail.SHORT_DESCRIPTION)
            title.nullOrFull()?.write(ListingDetail.TITLE)
            video.nullOrFull()?.write(ListingDetail.VIDEO)
        }

        private fun GppListing.fetchImages(rootDir: Directory) {
            for (type in ImageType.values()) {
                executor.noIsolation().submit(ImageFetcher::class) {
                    parameters.copy(this)

                    dir.set(rootDir.dir(GRAPHICS_PATH))
                    language.set(locale)
                    imageType.set(type)
                }
            }
        }

        interface Params : EditPublishingParams {
            val dir: DirectoryProperty
        }
    }

    abstract class ImageFetcher @Inject constructor(
            private val executor: WorkerExecutor,
    ) : EditWorkerBase<ImageFetcher.Params>() {
        override fun execute() {
            val typeName = parameters.imageType.get().publishedName
            val images = apiService.edits.getImages(parameters.language.get(), typeName)
            val imageDir = parameters.dir.get().dir(parameters.imageType.get().dirName)

            if (images.isEmpty()) return

            println("Downloading ${parameters.language.get()} listing graphics for type '$typeName'")
            for ((i, image) in images.withIndex()) {
                executor.noIsolation().submit(ImageDownloader::class) {
                    target.set(imageDir.file("${i + 1}"))
                    url.set(image.url)
                }
            }
        }

        interface Params : EditPublishingParams {
            val dir: DirectoryProperty
            val language: Property<String>
            val imageType: Property<ImageType>
        }
    }

    abstract class ImageDownloader : WorkAction<ImageDownloader.Params> {
        override fun execute() {
            val file = parameters.target.get().asFile

            file.safeCreateNewFile()
                    .outputStream()
                    .use { local ->
                        URL(parameters.url.get()).openStream().use { it.copyTo(local) }
                    }

            val magic = file.inputStream().use { it.readNBytes(KNOWN_IMAGE_TYPES.keys.maxOf { it.size }) }
            for ((possibleMagic, extension) in KNOWN_IMAGE_TYPES) {
                if (magic.size < possibleMagic.size) continue
                if (possibleMagic.withIndex().all { (i, b) -> magic[i] == b }) {
                    file.safeRenameTo(File(file.path + ".$extension"))
                    break
                }
            }
        }

        interface Params : WorkParameters {
            val target: RegularFileProperty
            val url: Property<String>
        }
    }

    abstract class ReleaseNotesDownloader :
            EditWorkerBase<ReleaseNotesDownloader.Params>() {
        override fun execute() {
            println("Downloading release notes")

            val notes = apiService.edits.getReleaseNotes()
            for (note in notes) {
                parameters.dir.get().file("${note.locale}/${note.track}.txt")
                        .write(note.contents)
            }
        }

        interface Params : EditPublishingParams {
            val dir: DirectoryProperty
        }
    }

    abstract class ProductsDownloader : EditWorkerBase<ProductsDownloader.Params>() {
        override fun execute() {
            println("Downloading in-app products")

            val products = apiService.publisher.getInAppProducts()
            for (product in products) {
                parameters.dir.get().file("${product.sku}.json").write(product.json)
            }
        }

        interface Params : EditPublishingParams {
            val dir: DirectoryProperty
        }
    }

    private companion object {
        val KNOWN_IMAGE_TYPES = mapOf(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) to "png",
                byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) to "jpg",
        )

        fun RegularFile.write(text: String) = asFile.safeCreateNewFile().writeText(text + "\n")
    }
}
