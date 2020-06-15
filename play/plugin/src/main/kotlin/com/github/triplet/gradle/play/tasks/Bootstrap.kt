package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.GppListing
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.GRAPHICS_PATH
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import com.github.triplet.gradle.play.tasks.internal.BootstrapOptions
import com.github.triplet.gradle.play.tasks.internal.PublishEditTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.EditWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.copy
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.net.URL
import javax.inject.Inject

internal abstract class Bootstrap @Inject constructor(
        extension: PlayPublisherExtension,
        appId: String,
        optionsHolder: BootstrapOptions.Holder
) : PublishEditTaskBase(extension, appId), BootstrapOptions by optionsHolder {
    @get:OutputDirectory
    abstract val srcDir: DirectoryProperty

    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun bootstrap() {
        project.delete(srcDir)

        val executor = project.serviceOf<WorkerExecutor>()
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
            val details = edits.getAppDetails()

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
            private val executor: WorkerExecutor
    ) : EditWorkerBase<ListingsDownloader.Params>() {
        override fun execute() {
            println("Downloading listings")
            val listings = edits.getListings()

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
            private val executor: WorkerExecutor
    ) : EditWorkerBase<ImageFetcher.Params>() {
        override fun execute() {
            val typeName = parameters.imageType.get().publishedName
            val images = edits.getImages(parameters.language.get(), typeName)
            val imageDir = parameters.dir.get().dir(parameters.imageType.get().dirName)

            if (images.isEmpty()) return

            println("Downloading ${parameters.language.get()} listing graphics for type '$typeName'")
            for ((i, image) in images.withIndex()) {
                executor.noIsolation().submit(ImageDownloader::class) {
                    target.set(imageDir.file("${i + 1}.png"))
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
            parameters.target.get().asFile.safeCreateNewFile()
                    .outputStream()
                    .use { local ->
                        URL(parameters.url.get()).openStream().use { it.copyTo(local) }
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

            val notes = edits.getReleaseNotes()
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

            val products = publisher.getInAppProducts()
            for (product in products) {
                parameters.dir.get().file("${product.sku}.json").write(product.json)
            }
        }

        interface Params : EditPublishingParams {
            val dir: DirectoryProperty
        }
    }

    private companion object {
        fun RegularFile.write(text: String) = asFile.safeCreateNewFile().writeText(text + "\n")
    }
}
