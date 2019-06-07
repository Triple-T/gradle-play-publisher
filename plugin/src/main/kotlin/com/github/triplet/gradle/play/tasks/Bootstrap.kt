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
import com.github.triplet.gradle.play.tasks.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.model.Listing
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.FileNotFoundException
import java.io.Serializable
import java.net.URL
import javax.inject.Inject

open class Bootstrap @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant,
        optionsHolder: BootstrapOptions.Holder
) : PlayPublishTaskBase(extension, variant), BootstrapOptions by optionsHolder {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
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

        val editId = getOrCreateEditId()

        val executor = project.serviceOf<WorkerExecutor>()
        if (downloadAppDetails) bootstrapAppDetails(executor, editId)
        if (downloadListings) bootstrapListings(executor, editId)
        if (downloadReleaseNotes) bootstrapReleaseNotes(executor, editId)
        if (downloadProducts) bootstrapProducts(executor)
    }

    private fun bootstrapAppDetails(executor: WorkerExecutor, editId: String) {
        executor.submit(DetailsDownloader::class) {
            paramsForBase(this, DetailsDownloader.Params(srcDir), editId)
        }
    }

    private fun bootstrapListings(executor: WorkerExecutor, editId: String) {
        executor.submit(ListingsDownloader::class) {
            isolationMode = IsolationMode.NONE
            paramsForBase(this, ListingsDownloader.Params(File(srcDir, LISTINGS_PATH)), editId)
        }
    }

    private fun bootstrapReleaseNotes(executor: WorkerExecutor, editId: String) {
        executor.submit(ReleaseNotesDownloader::class) {
            paramsForBase(
                    this, ReleaseNotesDownloader.Params(File(srcDir, RELEASE_NOTES_PATH)), editId)
        }
    }

    private fun bootstrapProducts(executor: WorkerExecutor) {
        executor.submit(ProductsDownloader::class) {
            paramsForBase(this, ProductsDownloader.Params(File(srcDir, PRODUCTS_PATH)))
        }
    }

    private class DetailsDownloader @Inject constructor(
            private val p: Params,
            data: PlayPublishingData
    ) : PlayWorkerBase(data) {
        override fun run() {
            println("Downloading app details")
            val details = edits.details().get(appId, editId).execute()

            details.contactEmail.nullOrFull()?.write(AppDetail.CONTACT_EMAIL)
            details.contactPhone.nullOrFull()?.write(AppDetail.CONTACT_PHONE)
            details.contactWebsite.nullOrFull()?.write(AppDetail.CONTACT_WEBSITE)
            details.defaultLanguage.nullOrFull()?.write(AppDetail.DEFAULT_LANGUAGE)
        }

        private fun String.write(detail: AppDetail) = write(p.dir, detail.fileName)

        data class Params(val dir: File) : Serializable
    }

    private class ListingsDownloader @Inject constructor(
            private val executor: WorkerExecutor,
            private val p: Params,
            private val data: PlayPublishingData
    ) : PlayWorkerBase(data) {
        override fun run() {
            println("Downloading listings")
            val listings = edits.listings().list(appId, editId).execute().listings ?: return

            for (listing in listings) {
                val rootDir = File(p.dir, listing.language)

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
                executor.submit(ImageFetcher::class) {
                    isolationMode = IsolationMode.NONE
                    params(ImageFetcher.Params(File(rootDir, GRAPHICS_PATH), language, type), data)
                }
            }
        }

        data class Params(val dir: File) : Serializable

        private class ImageFetcher @Inject constructor(
                private val executor: WorkerExecutor,
                private val p: Params,
                data: PlayPublishingData
        ) : PlayWorkerBase(data) {
            override fun run() {
                val typeName = p.type.publishedName
                val images = edits.images()
                        .list(appId, editId, p.language, typeName)
                        .execute()
                        .images ?: return
                val imageDir = File(p.dir, p.type.dirName).safeMkdirs()

                println("Downloading ${p.language} listing graphics for type '$typeName'")
                for ((i, image) in images.withIndex()) {
                    executor.submit(ImageDownloader::class) {
                        params(ImageDownloader.Params(File(imageDir, "${i + 1}.png"), image.url))
                    }
                }
            }

            data class Params(
                    val dir: File,
                    val language: String,
                    val type: ImageType
            ) : Serializable
        }

        private class ImageDownloader @Inject constructor(private val p: Params) : Runnable {
            override fun run() {
                p.target.safeCreateNewFile()
                        .outputStream()
                        .use { local ->
                            val remote = try {
                                URL(p.url + HIGH_RES_IMAGE_REQUEST).openStream()
                            } catch (e: FileNotFoundException) {
                                URL(p.url).openStream()
                            }

                            remote.use { it.copyTo(local) }
                        }
            }

            data class Params(val target: File, val url: String) : Serializable

            private companion object {
                const val HIGH_RES_IMAGE_REQUEST = "=h16383" // Max res: 2^14 - 1
            }
        }
    }

    private class ReleaseNotesDownloader @Inject constructor(
            private val p: Params,
            data: PlayPublishingData
    ) : PlayWorkerBase(data) {
        override fun run() {
            println("Downloading release notes")

            val tracks = edits.tracks().list(appId, editId).execute().tracks.orEmpty()
            for (track in tracks) {
                val notes = track.releases?.maxBy {
                    it.versionCodes?.max() ?: Long.MIN_VALUE
                }?.releaseNotes.orEmpty()

                for (note in notes) note.text.write(p.dir, "${note.language}/${track.track}.txt")
            }
        }

        data class Params(val dir: File) : Serializable
    }

    private class ProductsDownloader @Inject constructor(
            private val p: Params,
            data: PlayPublishingData
    ) : PlayWorkerBase(data) {
        override fun run() {
            println("Downloading in-app products")
            publisher.inappproducts().list(appId).execute().inappproduct?.forEach {
                JacksonFactory.getDefaultInstance()
                        .toPrettyString(it)
                        .write(p.dir, "${it.sku}.json")
            }
        }

        data class Params(val dir: File) : Serializable
    }

    private companion object {
        fun String.write(dir: File, fileName: String) =
                File(dir, fileName).safeCreateNewFile().writeText(this + "\n")
    }
}
