package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.ImageFileFilter
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTING_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.climbUpTo
import com.github.triplet.gradle.play.internal.isDirectChildOf
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.playPath
import com.github.triplet.gradle.play.internal.readProcessed
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

open class PublishListingTask : PlayPublishTaskBase() {
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    lateinit var resDir: File
    @Suppress("MemberVisibilityCanBePrivate") // Needed for Gradle caching to work correctly
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputFile
    val outputFile by lazy { File(project.buildDir, "${variant.playPath}/listing-cache-key") }

    @TaskAction
    fun publishListing(inputs: IncrementalTaskInputs) {
        if (!inputs.isIncremental) project.delete(outputs.files)

        var appDetailsChanged = false
        val changedListingDetails = mutableSetOf<File>()
        val changedImages = mutableListOf<Pair<ImageType, File>>()

        fun File.process() {
            appDetailsChanged = appDetailsChanged || invalidatesAppDetails()
            if (invalidatesListingDetails()) {
                climbUpTo(LISTING_PATH)?.let { changedListingDetails += it }
            }
            invalidatedImageType()?.let { changedImages += it to this }
        }

        inputs.outOfDate { it.file.process() }
        inputs.removed { it.file.process() }

        write { editId ->
            if (appDetailsChanged) updateAppDetails(editId)
            for (listingDir in changedListingDetails) {
                updateListing(editId, listingDir.parentFile.name, listingDir)
            }
            changedImages.map { (type, image) ->
                type to image.parentFile
            }.toSet().forEach { (type, imageDir) ->
                updateImages(
                        editId, imageDir.climbUpTo(LISTING_PATH)!!.parentFile.name, type, imageDir)
            }

            outputFile.writeText(editId)
        }
    }

    private fun AndroidPublisher.Edits.updateAppDetails(editId: String) {
        val details = AppDetails().apply {
            val errorOnSizeLimit = extension.errorOnSizeLimit

            defaultLanguage = File(resDir, AppDetail.DEFAULT_LANGUAGE.fileName).orNull()
                    ?.readProcessed(AppDetail.DEFAULT_LANGUAGE.maxLength, errorOnSizeLimit)
            contactEmail = File(resDir, AppDetail.CONTACT_EMAIL.fileName).orNull()
                    ?.readProcessed(AppDetail.CONTACT_EMAIL.maxLength, errorOnSizeLimit)
            contactPhone = File(resDir, AppDetail.CONTACT_PHONE.fileName).orNull()
                    ?.readProcessed(AppDetail.CONTACT_PHONE.maxLength, errorOnSizeLimit)
            contactWebsite = File(resDir, AppDetail.CONTACT_WEBSITE.fileName).orNull()
                    ?.readProcessed(AppDetail.CONTACT_WEBSITE.maxLength, errorOnSizeLimit)
        }

        details().update(variant.applicationId, editId, details).execute()
    }

    private fun AndroidPublisher.Edits.updateListing(
            editId: String,
            locale: String,
            listingDir: File
    ) {
        val listing = Listing().apply {
            val errorOnSizeLimit = extension.errorOnSizeLimit

            title = File(listingDir, ListingDetail.TITLE.fileName).orNull()
                    ?.readProcessed(ListingDetail.TITLE.maxLength, errorOnSizeLimit)
            shortDescription = File(listingDir, ListingDetail.SHORT_DESCRIPTION.fileName)
                    .orNull()
                    ?.readProcessed(ListingDetail.SHORT_DESCRIPTION.maxLength, errorOnSizeLimit)
            fullDescription = File(listingDir, ListingDetail.FULL_DESCRIPTION.fileName)
                    .orNull()
                    ?.readProcessed(ListingDetail.FULL_DESCRIPTION.maxLength, errorOnSizeLimit)
            video = File(listingDir, ListingDetail.VIDEO.fileName).orNull()
                    ?.readProcessed(ListingDetail.VIDEO.maxLength, errorOnSizeLimit)
        }

        try {
            listings().update(variant.applicationId, editId, locale, listing).execute()
        } catch (e: GoogleJsonResponseException) {
            if (e.details.errors.any { it.reason == "unsupportedListingLanguage" }) {
                // Rethrow for clarity
                throw IllegalArgumentException("Unsupported locale $locale", e)
            } else {
                throw e
            }
        }
    }

    private fun AndroidPublisher.Edits.updateImages(
            editId: String,
            locale: String,
            type: ImageType,
            imageDir: File
    ) {
        val typeName = type.fileName
        val files = imageDir.listFiles()
                ?.sorted()
                ?.map { FileContent(MIME_TYPE_IMAGE, it) } ?: return

        check(files.all {
            val isValidType = ImageFileFilter.accept(it.file)
            if (!isValidType) logger.error("Invalid file type: ${it.file.name}")
            isValidType
        }) { "Invalid files type(s), check logs for details." }
        check(files.size <= type.maxNum) {
            "You can only upload ${type.maxNum} graphic(s) for the $typeName"
        }

        images().deleteall(variant.applicationId, editId, locale, typeName).execute()
        for (file in files) {
            images()
                    .upload(variant.applicationId, editId, locale, typeName, file)
                    .execute()
        }
    }

    private fun File.invalidatesAppDetails() =
            isDirectChildOf(resDir.name) && AppDetail.values().any { it.fileName == name }

    private fun File.invalidatesListingDetails() =
            isDirectChildOf(LISTING_PATH) && ListingDetail.values().any { it.fileName == name }

    private fun File.invalidatedImageType() = ImageType.values().find { isDirectChildOf(it.fileName) }

    private companion object {
        const val MIME_TYPE_IMAGE = "image/*"
    }
}
