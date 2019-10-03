package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.has
import com.github.triplet.gradle.play.tasks.internal.EditTaskBase
import com.github.triplet.gradle.play.tasks.internal.buildPublisher
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.androidpublisher.AndroidPublisher
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class GenerateEdit @Inject constructor(
        extension: PlayPublisherExtension
) : EditTaskBase(extension) {
    init {
        // Always out-of-date since we need a new edit for every build. At some point, it would be
        // nice to run this only if dependent tasks are actually running (aka not up-to-date).
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun generate() {
        val file = editIdFile.get().asFile
        project.serviceOf<WorkerExecutor>().noIsolation().submit(Generator::class) {
            config.set(extension.serializableConfig)
            editIdFile.set(file)
        }
    }

    internal abstract class Generator : WorkAction<Generator.Params> {
        private val file = parameters.editIdFile.get().asFile
        private val appId = file.nameWithoutExtension

        override fun execute() {
            val editId = parameters.config.get().buildPublisher().getOrCreateEditId()
            file.safeCreateNewFile().writeText(editId)
        }

        private fun AndroidPublisher.getOrCreateEditId(): String = try {
            val editId = file.orNull()?.readText().nullOrFull()?.takeIf {
                file.marked("skipped").exists()
            }
            file.reset()

            if (editId == null) {
                edits().insert(appId, null).execute().id
            } else {
                file.marked("skipped").safeCreateNewFile()
                edits().get(appId, editId).execute().id
            }
        } catch (e: GoogleJsonResponseException) {
            when {
                e has "applicationNotFound" -> throw IllegalArgumentException(
                        // Rethrow for clarity
                        "No application found for the package name $appId. " +
                                "The first version of your app must be uploaded via the " +
                                "Play Store console.", e)
                e has "editAlreadyCommitted" || e has "editNotFound" || e has "editExpired" -> {
                    Logging.getLogger(GenerateEdit::class.java)
                            .error("Failed to retrieve saved edit, regenerating.")
                    getOrCreateEditId()
                }
                e.statusCode == 401 -> throw IllegalArgumentException(
                        "Service account not authenticated. See the README for instructions: " +
                                "https://github.com/Triple-T/gradle-play-publisher/" +
                                "blob/master/README.md#service-account", e)
                else -> throw e
            }
        }

        interface Params : WorkParameters {
            val config: Property<PlayPublisherExtension.Config>
            val editIdFile: RegularFileProperty
        }
    }
}
