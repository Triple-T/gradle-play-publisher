package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.GRAPHICS_PATH
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.readProcessed
import com.github.triplet.gradle.play.tasks.internal.EditWorkerBase
import com.github.triplet.gradle.play.tasks.internal.PlayPublishEditTaskBase
import com.github.triplet.gradle.play.tasks.internal.WriteTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.gradle.api.file.DirectoryProperty
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
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

abstract class PublishListing @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PlayPublishEditTaskBase(extension, variant), WriteTrackExtensionOptions {
    @get:Internal
    internal abstract val resDir: DirectoryProperty

    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val detailFiles: FileCollection by lazy {
        project.fileTree(resDir).builtBy(resDir).apply {
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
        project.fileTree(resDir).builtBy(resDir).apply {
            for (detail in ListingDetail.values()) include("/$LISTINGS_PATH/*/${detail.fileName}")
        }
    }
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val mediaFiles: FileCollection by lazy {
        project.fileTree(resDir).builtBy(resDir).apply {
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
        val details = processDetails(changes)
        val listings = processListings(changes)
        val media = processMedia(changes)

        if (details == null && listings.isEmpty() && media.isEmpty()) return

        project.serviceOf<WorkerExecutor>().submit(Publisher::class) {
            isolationMode = IsolationMode.NONE
            paramsForBase(this, Publisher.Params(details, listings, media))
        }
    }

    private fun processDetails(changes: InputChanges): DetailsUploader.Params? {
        val changedDetails =
                changes.getFileChanges(detailFiles).filter { it.fileType == FileType.FILE }
        if (changedDetails.isEmpty()) return null
        if (AppDetail.values().map { resDir.file(it.fileName) }.none { it.get().asFile.exists() }) {
            return null
        }

        return DetailsUploader.Params(resDir.asFile.get())
    }

    private fun processListings(changes: InputChanges): List<ListingUploader.Params> {
        val changedLocales = changes.getFileChanges(listingFiles).asSequence()
                .filter { it.fileType == FileType.FILE }
                .map { it.file.parentFile }
                .distinct()
                // We can't filter out FileType#REMOVED b/c otherwise we won't publish the changes
                .filter { it.exists() }
                .toList()

        return changedLocales.map { listingDir -> ListingUploader.Params(listingDir) }
    }

    private fun processMedia(changes: InputChanges): List<MediaUploader.Params> {
        val changedMediaTypes = changes.getFileChanges(mediaFiles).asSequence()
                .filter { it.fileType == FileType.FILE }
                .map { it.file.parentFile }
                .map { f -> f to ImageType.values().find { f.name == it.dirName } }
                .filter { it.second != null }
                .filter { it.first.exists() }
                .map { it.first to it.second!! }
                .associate { it }

        return changedMediaTypes.map { (imageDir, type) -> MediaUploader.Params(imageDir, type) }
    }

    private class Publisher @Inject constructor(
            private val executor: WorkerExecutor,

            private val p: Params,
            private val data: EditPublishingParams
    ) : EditWorkerBase(data) {
        override fun run() {
            if (p.details != null) {
                executor.submit(DetailsUploader::class) { params(p.details, data) }
            }
            for (listing in p.listings) {
                executor.submit(ListingUploader::class) { params(listing, data) }
            }
            for (medium in p.media) {
                executor.submit(MediaUploader::class) { params(medium, data) }
            }

            executor.await()
            commit()
        }

        data class Params(
                val details: DetailsUploader.Params?,
                val listings: List<ListingUploader.Params>,
                val media: List<MediaUploader.Params>
        ) : Serializable
    }

    private class DetailsUploader @Inject constructor(
            private val p: Params,
            data: EditPublishingParams
    ) : EditWorkerBase(data) {
        override fun run() {
            println("Uploading app details")
            val details = AppDetails().apply {
                fun AppDetail.read() = File(p.dir, fileName).orNull()?.readProcessed()

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
            data: EditPublishingParams
    ) : EditWorkerBase(data) {
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
            edits.listings().update(appId, editId, locale, listing).execute()
        }

        fun ListingDetail.read() = File(p.listingDir, fileName).orNull()?.readProcessed()

        data class Params(val listingDir: File) : Serializable
    }

    private class MediaUploader @Inject constructor(
            private val p: Params,
            data: EditPublishingParams
    ) : EditWorkerBase(data) {
        override fun run() {
            val typeName = p.type.publishedName
            val files = p.imageDir.listFiles()?.sorted() ?: return
            check(files.size <= p.type.maxNum) {
                "You can only upload ${p.type.maxNum} $typeName."
            }

            val locale = p.imageDir/*icon*/.parentFile/*graphics*/.parentFile/*en-US*/.name
            val remoteHashes = edits.images().list(appId, editId, locale, typeName).execute()
                    .images.orEmpty()
                    .map { it.sha256 }
            val localHashes = files.map {
                Files.asByteSource(it).hash(Hashing.sha256()).toString()
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
