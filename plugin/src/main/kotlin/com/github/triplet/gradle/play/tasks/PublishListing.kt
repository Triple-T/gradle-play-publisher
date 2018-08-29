package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.ImageFileFilter
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
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
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

open class PublishListing : PlayPublishTaskBase() {
    @get:Internal
    internal lateinit var resDir: File
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    protected val targetFiles: FileCollection by lazy {
        val appDetails = project.fileTree(resDir).apply {
            // We can't simply use `project.files` because Gradle would expect those to exist for
            // stuff like `@SkipWhenEmpty` to work.
            for (detail in AppDetail.values()) include("/${detail.fileName}")
        }
        val listings = project.fileTree(File(resDir, LISTINGS_PATH))

        appDetails + listings
    }
    @Suppress("MemberVisibilityCanBePrivate") // Needed for Gradle caching to work correctly
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:OutputFile
    internal val outputFile by lazy {
        File(project.buildDir, "${variant.playPath}/listing-cache-key")
    }

    @TaskAction
    fun publishListing(inputs: IncrementalTaskInputs) {
        if (!inputs.isIncremental) project.delete(outputs.files)

        var appDetailsChanged = false
        val changedListingDetails = mutableSetOf<File>()
        val changedImages = mutableListOf<Pair<ImageType, File>>()

        fun File.process() {
            appDetailsChanged = appDetailsChanged || invalidatesAppDetails()
            if (invalidatesListingDetails()) {
                changedListingDetails += findLocale()?.let { climbUpTo(it) }!!
            }
            invalidatedImageType()?.let { changedImages += it to this }
        }

        inputs.outOfDate { file.process() }
        inputs.removed { file.process() }

        write { editId ->
            progressLogger.start("Uploads app metadata for variant ${variant.name}", null)

            if (appDetailsChanged) updateAppDetails(editId)
            for (listingDir in changedListingDetails) {
                updateListing(editId, listingDir.name, listingDir)
            }
            changedImages.map { (type, image) ->
                type to image.parentFile
            }.toSet().forEach { (type, imageDir) ->
                updateImages(editId, imageDir.findLocale()!!, type, imageDir)
            }

            outputFile.writeText(editId)

            progressLogger.completed()
        }
    }

    private fun AndroidPublisher.Edits.updateAppDetails(editId: String) {
        progressLogger.progress("Uploading app details")
        val details = AppDetails().apply {
            fun AppDetail.read() = File(resDir, fileName).orNull()?.readProcessed(maxLength)

            defaultLanguage = AppDetail.DEFAULT_LANGUAGE.read()
            contactEmail = AppDetail.CONTACT_EMAIL.read()
            contactPhone = AppDetail.CONTACT_PHONE.read()
            contactWebsite = AppDetail.CONTACT_WEBSITE.read()
        }

        details().update(variant.applicationId, editId, details).execute()
    }

    private fun AndroidPublisher.Edits.updateListing(
            editId: String,
            locale: String,
            listingDir: File
    ) {
        progressLogger.progress("Uploading $locale listing")
        val listing = Listing().apply {
            fun ListingDetail.read() = File(listingDir, fileName).orNull()?.readProcessed(maxLength)

            title = ListingDetail.TITLE.read()
            shortDescription = ListingDetail.SHORT_DESCRIPTION.read()
            fullDescription = ListingDetail.FULL_DESCRIPTION.read()
            video = ListingDetail.VIDEO.read()
        }

        try {
            listings().update(variant.applicationId, editId, locale, listing).execute()
        } catch (e: GoogleJsonResponseException) {
            if (e.details?.errors.orEmpty().any { it.reason == "unsupportedListingLanguage" }) {
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
        val typeName = type.publishedName
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

        progressLogger.progress("Uploading $locale listing graphics for type '$typeName'")
        images().deleteall(variant.applicationId, editId, locale, typeName).execute()
        for (file in files) {
            images()
                    .upload(variant.applicationId, editId, locale, typeName, file)
                    .execute()
        }
    }

    private fun File.invalidatesAppDetails() =
            isDirectChildOf(resDir.name) && AppDetail.values().any { it.fileName == name }

    private fun File.invalidatesListingDetails(): Boolean {
        return isDirectChildOf(findLocale() ?: return false)
                && ListingDetail.values().any { it.fileName == name }
    }

    private fun File.invalidatedImageType() =
            ImageType.values().find { isDirectChildOf(it.dirName) }

    private fun File.findLocale() = climbUpTo(LISTINGS_PATH)
            ?.let { relativeTo(it).invariantSeparatorsPath }
            ?.split("/")
            ?.firstOrNull()

    private companion object {
        const val MIME_TYPE_IMAGE = "image/*"
    }
}
