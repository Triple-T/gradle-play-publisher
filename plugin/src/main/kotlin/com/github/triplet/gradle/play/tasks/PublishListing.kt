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
import com.github.triplet.gradle.play.tasks.internal.PublishEditTaskBase
import com.github.triplet.gradle.play.tasks.internal.WriteTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.copy
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
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

abstract class PublishListing @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PublishEditTaskBase(extension, variant), WriteTrackExtensionOptions {
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

        project.serviceOf<WorkerExecutor>().noIsolation().submit(Publisher::class) {
            paramsForBase(this)

            this.details.set(details)
            this.listings.set(listings)
            this.media.set(media)
        }
    }

    private fun processDetails(changes: InputChanges): File? {
        val changedDetails =
                changes.getFileChanges(detailFiles).filter { it.fileType == FileType.FILE }
        if (changedDetails.isEmpty()) return null
        if (AppDetail.values().map { resDir.file(it.fileName) }.none { it.get().asFile.exists() }) {
            return null
        }

        return resDir.asFile.get()
    }

    private fun processListings(
            changes: InputChanges
    ) = changes.getFileChanges(listingFiles).asSequence()
            .filter { it.fileType == FileType.FILE }
            .map { it.file.parentFile }
            .distinct()
            // We can't filter out FileType#REMOVED b/c otherwise we won't publish the changes
            .filter { it.exists() }
            .toList()

    private fun processMedia(
            changes: InputChanges
    ) = changes.getFileChanges(mediaFiles).asSequence()
            .filter { it.fileType == FileType.FILE }
            .map { it.file.parentFile }
            .map { f -> f to ImageType.values().find { f.name == it.dirName } }
            .filter { it.second != null }
            .filter { it.first.exists() }
            .map { it.first to it.second!! }
            .associate { it }
            .map { Publisher.Media(it.key, it.value) }

    internal abstract class Publisher @Inject constructor(
            private val executor: WorkerExecutor
    ) : EditWorkerBase<Publisher.Params>() {
        override fun execute() {
            if (parameters.details.isPresent) {
                executor.noIsolation().submit(DetailsUploader::class) {
                    parameters.copy(this)
                    dir.set(parameters.details.get())
                }
            }
            for (listing in parameters.listings.get()) {
                executor.noIsolation().submit(ListingUploader::class) {
                    parameters.copy(this)
                    listingDir.set(listing)
                }
            }
            for (medium in parameters.media.get()) {
                executor.noIsolation().submit(MediaUploader::class) {
                    parameters.copy(this)

                    imageDir.set(medium.imageDir)
                    imageType.set(medium.type)
                }
            }

            executor.await()
            commit()
        }

        interface Params : EditPublishingParams {
            val details: Property<File?>
            val listings: Property<List<File>>
            val media: Property<List<Media>>
        }

        data class Media(val imageDir: File, val type: ImageType) : Serializable
    }

    internal abstract class DetailsUploader : EditWorkerBase<DetailsUploader.Params>() {
        override fun execute() {
            println("Uploading app details")
            val details = AppDetails().apply {
                fun AppDetail.read() =
                        File(parameters.dir.get(), fileName).orNull()?.readProcessed()

                defaultLanguage = AppDetail.DEFAULT_LANGUAGE.read()
                contactEmail = AppDetail.CONTACT_EMAIL.read()
                contactPhone = AppDetail.CONTACT_PHONE.read()
                contactWebsite = AppDetail.CONTACT_WEBSITE.read()
            }

            edits.details().update(appId, editId, details).execute()
        }

        interface Params : EditPublishingParams {
            val dir: Property<File>
        }
    }

    internal abstract class ListingUploader : EditWorkerBase<ListingUploader.Params>() {
        override fun execute() {
            val locale = parameters.listingDir.get().name
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

        private fun ListingDetail.read() =
                File(parameters.listingDir.get(), fileName).orNull()?.readProcessed()

        interface Params : EditPublishingParams {
            val listingDir: Property<File>
        }
    }

    internal abstract class MediaUploader : EditWorkerBase<MediaUploader.Params>() {
        override fun execute() {
            val typeName = parameters.imageType.get().publishedName
            val files = parameters.imageDir.get().listFiles()?.sorted() ?: return
            check(files.size <= parameters.imageType.get().maxNum) {
                "You can only upload ${parameters.imageType.get().maxNum} $typeName."
            }

            val locale =
                    parameters.imageDir.get()/*icon*/.parentFile/*graphics*/.parentFile/*en-US*/.name
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

        interface Params : EditPublishingParams {
            val imageDir: Property<File>
            val imageType: Property<ImageType>
        }

        private companion object {
            const val MIME_TYPE_IMAGE = "image/*"
        }
    }
}
