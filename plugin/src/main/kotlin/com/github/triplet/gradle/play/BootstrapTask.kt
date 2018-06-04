package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTING_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.TrackType
import com.github.triplet.gradle.play.internal.nullOrFull
import com.github.triplet.gradle.play.internal.safeCreateNewFile
import com.google.api.services.androidpublisher.AndroidPublisher
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL

open class BootstrapTask : PlayPublishTaskBase() {
    lateinit var outputFolder: File

    @TaskAction
    fun bootstrap() = read { editId ->
        bootstrapListing(editId)
        bootstrapWhatsNew(editId)
        bootstrapAppDetails(editId)
    }

    private fun AndroidPublisher.Edits.bootstrapListing(editId: String) {
        val listings = listings()
                .list(variant.applicationId, editId)
                .execute()
                .listings ?: return

        for (listing in listings) {
            val rootDir = File(outputFolder, "${listing.language}/$LISTING_PATH")

            fun downloadMetadata() {
                fun String.write(detail: ListingDetail) = write(rootDir, detail)

                listing.fullDescription.nullOrFull()?.write(ListingDetail.FULL_DESCRIPTION)
                listing.shortDescription.nullOrFull()?.write(ListingDetail.SHORT_DESCRIPTION)
                listing.title.nullOrFull()?.write(ListingDetail.TITLE)
                listing.video.nullOrFull()?.write(ListingDetail.VIDEO)
            }

            fun downloadImages() {
                for (type in ImageType.values()) {
                    val images = images()
                            .list(variant.applicationId, editId, listing.language, type.fileName)
                            .execute()
                            .images ?: continue
                    val imageDir = File(rootDir, type.fileName)

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

    private fun AndroidPublisher.Edits.bootstrapWhatsNew(editId: String) = tracks()
            .list(variant.applicationId, editId)
            .execute().tracks
            ?.maxBy { TrackType.fromString(it.track) }
            ?.releases
            ?.maxBy { it.versionCodes.max() ?: Long.MIN_VALUE }
            ?.releaseNotes
            ?.forEach {
                File(outputFolder, "${it.language}/${ListingDetail.WHATS_NEW.fileName}")
                        .safeCreateNewFile()
                        .writeText(it.text)
            }

    private fun AndroidPublisher.Edits.bootstrapAppDetails(editId: String) {
        fun String.write(detail: ListingDetail) = write(outputFolder, detail)

        val details = details().get(variant.applicationId, editId).execute()

        details.contactEmail.nullOrFull()?.write(ListingDetail.CONTACT_EMAIL)
        details.contactPhone.nullOrFull()?.write(ListingDetail.CONTACT_PHONE)
        details.contactWebsite.nullOrFull()?.write(ListingDetail.CONTACT_WEBSITE)
        details.defaultLanguage.nullOrFull()?.write(ListingDetail.DEFAULT_LANGUAGE)
    }

    private fun String.write(dir: File, detail: ListingDetail) =
            File(dir, detail.fileName).safeCreateNewFile().writeText(this)
}
