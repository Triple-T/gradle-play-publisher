package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.GRAPHICS_PATH
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.internal.PLAY_PATH
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import com.github.triplet.gradle.play.internal.flavorNameOrDefault
import com.github.triplet.gradle.play.internal.nullOrFull
import com.github.triplet.gradle.play.internal.safeCreateNewFile
import com.github.triplet.gradle.play.internal.safeMkdirs
import com.github.triplet.gradle.play.tasks.internal.BootstrapOptions
import com.github.triplet.gradle.play.tasks.internal.EditWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PublishEditTaskBase
import com.github.triplet.gradle.play.tasks.internal.copy
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.model.Listing
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import javax.inject.Inject

abstract class Bootstrap @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant,
        optionsHolder: BootstrapOptions.Holder
) : PublishEditTaskBase(extension, variant), BootstrapOptions by optionsHolder {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:OutputDirectory
    protected val srcDir: File by lazy {
        project.file("src/${variant.flavorNameOrDefault}/$PLAY_PATH")
    }

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
            dir.set(File(srcDir, LISTINGS_PATH))
        }
    }

    private fun bootstrapReleaseNotes(executor: WorkerExecutor) {
        executor.noIsolation().submit(ReleaseNotesDownloader::class) {
            paramsForBase(this)
            dir.set(File(srcDir, RELEASE_NOTES_PATH))
        }
    }

    private fun bootstrapProducts(executor: WorkerExecutor) {
        executor.noIsolation().submit(ProductsDownloader::class) {
            paramsForBase(this)
            dir.set(File(srcDir, PRODUCTS_PATH))
        }
    }

    internal abstract class DetailsDownloader : EditWorkerBase<DetailsDownloader.Params>() {
        override fun execute() {
            println("Downloading app details")
            val details = edits.details().get(appId, editId).execute()

            details.contactEmail.nullOrFull()?.write(AppDetail.CONTACT_EMAIL)
            details.contactPhone.nullOrFull()?.write(AppDetail.CONTACT_PHONE)
            details.contactWebsite.nullOrFull()?.write(AppDetail.CONTACT_WEBSITE)
            details.defaultLanguage.nullOrFull()?.write(AppDetail.DEFAULT_LANGUAGE)
        }

        private fun String.write(detail: AppDetail) = write(parameters.dir.get(), detail.fileName)

        interface Params : EditPublishingParams {
            val dir: Property<File>
        }
    }

    internal abstract class ListingsDownloader @Inject constructor(
            private val executor: WorkerExecutor
    ) : EditWorkerBase<ListingsDownloader.Params>() {
        override fun execute() {
            println("Downloading listings")
            val listings = edits.listings().list(appId, editId).execute().listings ?: return

            for (listing in listings) {
                val rootDir = File(parameters.dir.get(), listing.language)

                listing.writeMetadata(rootDir)
                listing.fetchImages(rootDir)
            }
        }

        private fun Listing.writeMetadata(rootDir: File) {
            fun String.write(detail: ListingDetail) = write(rootDir, detail.fileName)

            println("Downloading $language listing")
            fullDescription.nullOrFull()?.write(ListingDetail.FULL_DESCRIPTION)
            shortDescription.nullOrFull()?.write(ListingDetail.SHORT_DESCRIPTION)
            title.nullOrFull()?.write(ListingDetail.TITLE)
            video.nullOrFull()?.write(ListingDetail.VIDEO)
        }

        private fun Listing.fetchImages(rootDir: File) {
            for (type in ImageType.values()) {
                executor.noIsolation().submit(ImageFetcher::class) {
                    parameters.copy(this)

                    dir.set(File(rootDir, GRAPHICS_PATH))
                    language.set(this@fetchImages.language)
                    imageType.set(type)
                }
            }
        }

        interface Params : EditPublishingParams {
            val dir: Property<File>
        }
    }

    internal abstract class ImageFetcher @Inject constructor(
            private val executor: WorkerExecutor
    ) : EditWorkerBase<ImageFetcher.Params>() {
        override fun execute() {
            val typeName = parameters.imageType.get().publishedName
            val images = edits.images()
                    .list(appId, editId, parameters.language.get(), typeName)
                    .execute()
                    .images ?: return
            val imageDir =
                    File(parameters.dir.get(), parameters.imageType.get().dirName).safeMkdirs()

            println("Downloading ${parameters.language.get()} listing graphics for type '$typeName'")
            for ((i, image) in images.withIndex()) {
                executor.noIsolation().submit(ImageDownloader::class) {
                    target.set(File(imageDir, "${i + 1}.png"))
                    url.set(image.url)
                }
            }
        }

        interface Params : EditPublishingParams {
            val dir: Property<File>
            val language: Property<String>
            val imageType: Property<ImageType>
        }
    }

    internal abstract class ImageDownloader : WorkAction<ImageDownloader.Params> {
        override fun execute() {
            parameters.target.get().safeCreateNewFile()
                    .outputStream()
                    .use { local ->
                        val remote = try {
                            URL(parameters.url.get() + HIGH_RES_IMAGE_REQUEST).openStream()
                        } catch (e: FileNotFoundException) {
                            URL(parameters.url.get()).openStream()
                        }

                        remote.use { it.copyTo(local) }
                    }
        }

        interface Params : WorkParameters {
            val target: Property<File>
            val url: Property<String>
        }

        private companion object {
            const val HIGH_RES_IMAGE_REQUEST = "=h16383" // Max res: 2^14 - 1
        }
    }

    internal abstract class ReleaseNotesDownloader :
            EditWorkerBase<ReleaseNotesDownloader.Params>() {
        override fun execute() {
            println("Downloading release notes")

            val tracks = edits.tracks().list(appId, editId).execute().tracks.orEmpty()
            for (track in tracks) {
                val notes = track.releases?.maxBy {
                    it.versionCodes?.max() ?: Long.MIN_VALUE
                }?.releaseNotes.orEmpty()

                for (note in notes) {
                    note.text.write(parameters.dir.get(), "${note.language}/${track.track}.txt")
                }
            }
        }

        interface Params : EditPublishingParams {
            val dir: Property<File>
        }
    }

    internal abstract class ProductsDownloader : EditWorkerBase<ProductsDownloader.Params>() {
        override fun execute() {
            println("Downloading in-app products")
            publisher.inappproducts().list(appId).execute().inappproduct?.forEach {
                JacksonFactory.getDefaultInstance()
                        .toPrettyString(it)
                        .write(parameters.dir.get(), "${it.sku}.json")
            }
        }

        interface Params : EditPublishingParams {
            val dir: Property<File>
        }
    }

    private companion object {
        fun String.write(dir: File, fileName: String) =
                File(dir, fileName).safeCreateNewFile().writeText(this + "\n")
    }
}
