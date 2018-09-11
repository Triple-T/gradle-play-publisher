package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.GRAPHICS_PATH
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.internal.PRODUCTS_PATH
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.RELEASE_NOTES_PATH
import com.github.triplet.gradle.play.internal.nullOrFull
import com.github.triplet.gradle.play.internal.safeCreateNewFile
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL

open class Bootstrap : PlayPublishTaskBase() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputDirectory
    internal lateinit var srcDir: File

    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun bootstrap() = read { editId ->
        progressLogger.start("Downloads resources for variant ${variant.name}", null)

        bootstrapAppDetails(editId)
        bootstrapListing(editId)
        bootstrapReleaseNotes(editId)
        bootstrapProducts()

        progressLogger.completed()
    }

    private fun AndroidPublisher.Edits.bootstrapAppDetails(editId: String) {
        fun String.write(detail: AppDetail) = write(srcDir, detail.fileName)

        progressLogger.progress("Downloading app details")
        val details = details().get(variant.applicationId, editId).execute()

        details.contactEmail.nullOrFull()?.write(AppDetail.CONTACT_EMAIL)
        details.contactPhone.nullOrFull()?.write(AppDetail.CONTACT_PHONE)
        details.contactWebsite.nullOrFull()?.write(AppDetail.CONTACT_WEBSITE)
        details.defaultLanguage.nullOrFull()?.write(AppDetail.DEFAULT_LANGUAGE)
    }

    private fun AndroidPublisher.Edits.bootstrapListing(editId: String) {
        progressLogger.progress("Fetching listings")
        val listings = listings()
                .list(variant.applicationId, editId)
                .execute()
                .listings ?: return

        for (listing in listings) {
            val rootDir = File(srcDir, "$LISTINGS_PATH/${listing.language}")

            fun downloadMetadata() {
                fun String.write(detail: ListingDetail) = write(rootDir, detail.fileName)

                progressLogger.progress("Downloading ${listing.language} listing")
                listing.fullDescription.nullOrFull()?.write(ListingDetail.FULL_DESCRIPTION)
                listing.shortDescription.nullOrFull()?.write(ListingDetail.SHORT_DESCRIPTION)
                listing.title.nullOrFull()?.write(ListingDetail.TITLE)
                listing.video.nullOrFull()?.write(ListingDetail.VIDEO)
            }

            fun downloadImages() {
                for (type in ImageType.values()) {
                    val typeName = type.publishedName
                    progressLogger.progress(
                            "Downloading ${listing.language} listing graphics for type '$typeName'")
                    val images = images()
                            .list(variant.applicationId, editId, listing.language, typeName)
                            .execute()
                            .images ?: continue
                    val imageDir = File(rootDir, "$GRAPHICS_PATH/${type.dirName}")

                    for (image in images) {
                        File(imageDir, "${image.id}.png")
                                .safeCreateNewFile()
                                .outputStream()
                                .use { stream ->
                                    URL(image.url).openStream().use { it.copyTo(stream) }
                                }
                    }
                }
            }

            downloadMetadata()
            downloadImages()
        }
    }

    private fun AndroidPublisher.Edits.bootstrapReleaseNotes(editId: String) {
        progressLogger.progress("Downloading release notes")
        tracks().list(variant.applicationId, editId).execute().tracks?.forEach { track ->
            track.releases.maxBy {
                it.versionCodes?.max() ?: Long.MIN_VALUE
            }?.releaseNotes?.forEach {
                File(srcDir, "$RELEASE_NOTES_PATH/${it.language}/${track.track}.txt")
                        .safeCreateNewFile()
                        .writeText(it.text)
            }
        }
    }

    private fun bootstrapProducts() {
        progressLogger.progress("Downloading in-app products")
        publisher.inappproducts().list(variant.applicationId).execute().inappproduct.forEach {
            JacksonFactory.getDefaultInstance()
                    .toPrettyString(it)
                    .write(srcDir, "$PRODUCTS_PATH/${it.sku}.json")
        }
    }

    private fun String.write(dir: File, fileName: String) =
            File(dir, fileName).safeCreateNewFile().writeText(this)
}
