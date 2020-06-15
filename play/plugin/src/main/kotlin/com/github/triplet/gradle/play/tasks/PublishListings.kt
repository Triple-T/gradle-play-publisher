package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.common.utils.readProcessed
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.AppDetail
import com.github.triplet.gradle.play.internal.GRAPHICS_PATH
import com.github.triplet.gradle.play.internal.ImageType
import com.github.triplet.gradle.play.internal.LISTINGS_PATH
import com.github.triplet.gradle.play.internal.ListingDetail
import com.github.triplet.gradle.play.tasks.internal.PublishEditTaskBase
import com.github.triplet.gradle.play.tasks.internal.WriteTrackExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.workers.EditWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.copy
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
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

internal abstract class PublishListings @Inject constructor(
        extension: PlayPublisherExtension,
        appId: String
) : PublishEditTaskBase(extension, appId), WriteTrackExtensionOptions {
    @get:Internal
    internal abstract val resDir: DirectoryProperty

    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val detailFiles: FileCollection by lazy {
        resDir.asFileTree.matching {
            // We can't simply use `project.files` because Gradle would expect those to exist for
            // stuff like `@SkipWhenEmpty` to work.
            for (detail in AppDetail.values()) include("/${detail.fileName}")
        }
    }

    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val listingFiles: FileCollection by lazy {
        resDir.asFileTree.matching {
            for (detail in ListingDetail.values()) include("/$LISTINGS_PATH/*/${detail.fileName}")
        }
    }

    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val mediaFiles: FileCollection by lazy {
        resDir.asFileTree.matching {
            for (image in ImageType.values()) {
                include("/$LISTINGS_PATH/*/$GRAPHICS_PATH/${image.dirName}/*")
            }
        }
    }

    // Used by Gradle to skip the task if all inputs are empty
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    protected val targetFiles: FileCollection by lazy { detailFiles + listingFiles + mediaFiles }

    // This directory isn't used, but it's needed for up-to-date checks to work
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:Optional
    @get:OutputDirectory
    protected val outputDir = null

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

    private fun processDetails(changes: InputChanges): Directory? {
        val changedDetails =
                changes.getFileChanges(detailFiles).filter { it.fileType == FileType.FILE }
        if (changedDetails.isEmpty()) return null
        if (AppDetail.values().map { resDir.file(it.fileName) }.none { it.get().asFile.exists() }) {
            return null
        }

        return resDir.get()
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
                    dir.set(parameters.details)
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
            val details: DirectoryProperty // Optional
            val listings: ListProperty<File>
            val media: ListProperty<Media>
        }

        data class Media(val imageDir: File, val type: ImageType) : Serializable
    }

    internal abstract class DetailsUploader : EditWorkerBase<DetailsUploader.Params>() {
        override fun execute() {
            val defaultLanguage = AppDetail.DEFAULT_LANGUAGE.read()
            val contactEmail = AppDetail.CONTACT_EMAIL.read()
            val contactPhone = AppDetail.CONTACT_PHONE.read()
            val contactWebsite = AppDetail.CONTACT_WEBSITE.read()

            println("Uploading app details")
            edits.publishAppDetails(defaultLanguage, contactEmail, contactPhone, contactWebsite)
        }

        private fun AppDetail.read() =
                parameters.dir.get().file(fileName).asFile.orNull()?.readProcessed()

        interface Params : EditPublishingParams {
            val dir: DirectoryProperty
        }
    }

    internal abstract class ListingUploader : EditWorkerBase<ListingUploader.Params>() {
        override fun execute() {
            val locale = parameters.listingDir.get().asFile.name
            val title = ListingDetail.TITLE.read()
            val shortDescription = ListingDetail.SHORT_DESCRIPTION.read()
            val fullDescription = ListingDetail.FULL_DESCRIPTION.read()
            val video = ListingDetail.VIDEO.read()
            if (title == null &&
                    shortDescription == null &&
                    fullDescription == null &&
                    video == null) {
                return
            }

            println("Uploading $locale listing")
            edits.publishListing(locale, title, shortDescription, fullDescription, video)
        }

        private fun ListingDetail.read() =
                parameters.listingDir.get().file(fileName).asFile.orNull()?.readProcessed()

        interface Params : EditPublishingParams {
            val listingDir: DirectoryProperty
        }
    }

    internal abstract class MediaUploader : EditWorkerBase<MediaUploader.Params>() {
        override fun execute() {
            val typeName = parameters.imageType.get().publishedName
            val files = parameters.imageDir.asFileTree.sorted()
                    .filterNot { it.extension == "index" }
            check(files.size <= parameters.imageType.get().maxNum) {
                "You can only upload ${parameters.imageType.get().maxNum} $typeName."
            }

            val locale = parameters.imageDir.get().asFile // icon
                    .parentFile // graphics
                    .parentFile // en-US
                    .name
            val remoteHashes = edits.getImages(locale, typeName).map { it.sha256 }
            val localHashes = files.map {
                Files.asByteSource(it).hash(Hashing.sha256()).toString()
            }
            if (remoteHashes == localHashes) return

            edits.publishImages(locale, typeName, files)
        }

        interface Params : EditPublishingParams {
            val imageDir: DirectoryProperty
            val imageType: Property<ImageType>
        }
    }
}
