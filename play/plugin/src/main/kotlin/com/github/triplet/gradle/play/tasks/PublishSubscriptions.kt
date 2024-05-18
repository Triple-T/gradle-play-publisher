package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.SubscriptionMetadata
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import com.google.api.client.json.gson.GsonFactory
import com.google.gson.Gson
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.work.ChangeType
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class PublishSubscriptions @Inject constructor(
        extension: PlayPublisherExtension,
        private val executor: WorkerExecutor,
) : PublishTaskBase(extension) {
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val subscriptionsDir: ConfigurableFileCollection

    // Used by Gradle to skip the task if all inputs are empty
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    protected val targetFiles: FileCollection by lazy { subscriptionsDir.asFileTree }

    // This directory isn't used, but it's needed for up-to-date checks to work
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:Optional
    @get:OutputDirectory
    protected val outputDir = null

    @TaskAction
    fun publishSubscriptions(changes: InputChanges) {
        changes.getFileChanges(subscriptionsDir)
                .filterNot { it.changeType == ChangeType.REMOVED }
                .filter { it.fileType == FileType.FILE }
                .map {
                    // We should attempt to publish a subscription if it or its metadata changes.
                    if (it.file.name.endsWith(SUBSCRIPTION_METADATA_SUFFIX)) {
                        it.file.parentFile.resolve(
                                it.file.name.dropLast(SUBSCRIPTION_METADATA_SUFFIX.length) + ".json")
                    } else {
                        it.file
                    }
                }
                .distinct()
                .forEach {
                    executor.noIsolation().submit(Uploader::class) {
                        paramsForBase(this)
                        target.set(it)
                    }
                }
    }

    abstract class Uploader : PlayWorkerBase<Uploader.Params>() {
        override fun execute() {
            val subscriptionFile = parameters.target.get().asFile

            val subscription = subscriptionFile.inputStream().use {
                GsonFactory.getDefaultInstance().createJsonParser(it).parse(Map::class.java)
            }

            val metadata = subscriptionFile.parentFile
                    .resolve(subscriptionFile.name.dropLast(".json".length) + SUBSCRIPTION_METADATA_SUFFIX).inputStream().use {
                        Gson().fromJson(it.reader(), SubscriptionMetadata::class.java)
                    }

            println("Uploading ${subscription["productId"]}")
            val response = apiService.publisher.updateInAppSubscription(subscriptionFile, metadata.regionsVersion)
            if (response.needsCreating) apiService.publisher.insertInAppSubscription(subscriptionFile, metadata.regionsVersion)
        }

        interface Params : PlayPublishingParams {
            val target: RegularFileProperty
        }
    }

    companion object {
        /** The file name suffix for subscription metadata files. */
        const val SUBSCRIPTION_METADATA_SUFFIX: String = ".metadata.json"
    }
}
