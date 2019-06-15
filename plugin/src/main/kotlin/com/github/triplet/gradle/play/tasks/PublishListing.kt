package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.GRAPHICS_PATH
import com.github.triplet.gradle.play.internal.ImageFileFilter
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.internal.has
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.readProcessed
import com.github.triplet.gradle.play.tasks.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.WriteTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

open class PublishListing @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PlayPublishTaskBase(extension, variant), WriteTrackExtensionOptions {
    @get:Internal
    internal lateinit var resDir: File

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val detailFiles: FileCollection by lazy {
        project.fileTree(resDir).apply {
            // We can't simply use `project.files` because Gradle would expect those to exist for
            // stuff like `@SkipWhenEmpty` to work.
            for (detail in AppDetail.values()) include("/${detail.fileName}")
        }
    }
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val listingFiles: FileCollection by lazy {
        project.fileTree(resDir).apply {
            for (detail in ListingDetail.values()) include("/$LISTINGS_PATH/*/${detail.fileName}")
        }
    }
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val mediaFiles: FileCollection by lazy {
        project.fileTree(resDir).apply {
            for (image in ImageType.values()) {
                include("/$LISTINGS_PATH/*/$GRAPHICS_PATH/${image.dirName}/*")
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    protected val targetFiles: FileCollection by lazy { detailFiles + listingFiles + mediaFiles }

    @TaskAction
    fun publishListing(changes: InputChanges) {
        val editId = getOrCreateEditId()

        val executor = project.serviceOf<WorkerExecutor>()
        processDetails(executor, changes, editId)
        processListings(executor, changes, editId)
        processMedia(executor, changes, editId)
        executor.await()

        commit(editId)
    }

    private fun processDetails(executor: WorkerExecutor, changes: InputChanges, editId: String) {
        val changedDetails =
                changes.getFileChanges(detailFiles).filter { it.fileType == FileType.FILE }
        if (changedDetails.isEmpty()) return
        if (AppDetail.values().map { File(resDir, it.fileName) }.none { it.exists() }) return

        executor.submit(DetailsUploader::class) {
            paramsForBase(this, DetailsUploader.Params(resDir), editId)
        }
    }

    private fun processListings(executor: WorkerExecutor, changes: InputChanges, editId: String) {
        val changedLocales = changes.getFileChanges(listingFiles).asSequence()
                .filter { it.fileType == FileType.FILE }
                .map { it.file.parentFile }
                .distinct()
                // We can't filter out FileType#REMOVED b/c otherwise we won't publish the changes
                .filter { it.exists() }
                .toList()

        for (listingDir in changedLocales) {
            executor.submit(ListingUploader::class) {
                paramsForBase(this, ListingUploader.Params(listingDir), editId)
            }
        }
    }

    private fun processMedia(executor: WorkerExecutor, changes: InputChanges, editId: String) {
        val changedMediaTypes = changes.getFileChanges(mediaFiles).asSequence()
                .filter { it.fileType == FileType.FILE }
                .map { it.file.parentFile }
                .map { f -> f to ImageType.values().find { f.name == it.dirName } }
                .filter { it.second != null }
                .filter { it.first.exists() }
                .map { it.first to it.second!! }
                .associate { it }

        for ((imageDir, type) in changedMediaTypes) {
            executor.submit(MediaUploader::class) {
                paramsForBase(this, MediaUploader.Params(imageDir, type), editId)
            }
        }
    }

    private class DetailsUploader @Inject constructor(
            private val p: Params,
            data: PlayPublishingData
    ) : PlayWorkerBase(data) {
        override fun run() {
            println("Uploading app details")
            val details = AppDetails().apply {
                fun AppDetail.read() = File(p.dir, fileName).orNull()?.readProcessed(maxLength)

                defaultLanguage = AppDetail.DEFAULT_LANGUAGE.read()
                contactEmail = AppDetail.CONTACT_EMAIL.read()
                contactPhone = AppDetail.CONTACT_PHONE.read()
                contactWebsite = AppDetail.CONTACT_WEBSITE.read()
            }

            edits.details().update(appId, editId, details).execute()
        }

        data class Params(val dir: File) : Serializable
    }

    private class ListingUploader @Inject constructor(
            private val p: Params,
            data: PlayPublishingData
    ) : PlayWorkerBase(data) {
        override fun run() {
            val locale = p.listingDir.name
            val listing = Listing().apply {
                title = ListingDetail.TITLE.read()
                shortDescription = ListingDetail.SHORT_DESCRIPTION.read()
                fullDescription = ListingDetail.FULL_DESCRIPTION.read()
                video = ListingDetail.VIDEO.read()
            }
            if (listing.toPrettyString() == "{}") return

            println("Uploading $locale listing")
            try {
                edits.listings().update(appId, editId, locale, listing).execute()
            } catch (e: GoogleJsonResponseException) {
                if (e has "unsupportedListingLanguage") {
                    // Rethrow for clarity
                    throw IllegalArgumentException("Unsupported locale: $locale", e)
                } else {
                    throw e
                }
            }
        }

        fun ListingDetail.read() = File(p.listingDir, fileName).orNull()?.readProcessed(maxLength)

        data class Params(val listingDir: File) : Serializable
    }

    private class MediaUploader @Inject constructor(
            private val p: Params,
            data: PlayPublishingData
    ) : PlayWorkerBase(data) {
        override fun run() {
            val typeName = p.type.publishedName
            val files = p.imageDir.listFiles()?.sorted() ?: return

            check(files.all {
                val isValidType = ImageFileFilter.accept(it)
                if (!isValidType) logger.error("Invalid file type: ${it.name}")
                isValidType
            }) { "Invalid files type(s), check logs for details." }
            check(files.size <= p.type.maxNum) {
                "You can only upload ${p.type.maxNum} graphic(s) for the $typeName"
            }

            val locale = p.imageDir/*icon*/.parentFile/*graphics*/.parentFile/*en-US*/.name
            val remoteHashes = edits.images().list(appId, editId, locale, typeName).execute()
                    .images.orEmpty()
                    .map { it.sha1 }
            val localHashes = files.map {
                @Suppress("DEPRECATION") // The API only provides SHA1 hashes
                Files.asByteSource(it).hash(Hashing.sha1()).toString()
            }
            if (remoteHashes == localHashes) return

            edits.images().deleteall(appId, editId, locale, typeName).execute()
            for (file in files) {
                println("Uploading $locale listing graphic for type '$typeName': ${file.name}")
                // These can't be uploaded in parallel because order matters
                edits.images().upload(
                        appId,
                        editId,
                        locale,
                        typeName,
                        FileContent(MIME_TYPE_IMAGE, file)
                ).execute()
            }
        }

        data class Params(val imageDir: File, val type: ImageType) : Serializable

        private companion object {
            const val MIME_TYPE_IMAGE = "image/*"
        }
    }
}
