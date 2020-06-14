package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.PlayExtensionConfig
import com.github.triplet.gradle.play.internal.credentialStream
import com.github.triplet.gradle.play.internal.toConfig
import com.github.triplet.gradle.play.tasks.internal.EditTaskBase
import org.gradle.api.file.FileSystemOperations
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

internal abstract class GenerateEdit @Inject constructor(
        extension: PlayPublisherExtension
) : EditTaskBase(extension) {
    init {
        // Always out-of-date since we need a new edit for every build. At some point, it would be
        // nice to run this only if dependent tasks are actually running (aka not up-to-date).
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun generate() {
        val editId = editIdFile
        project.serviceOf<WorkerExecutor>().noIsolation().submit(Generator::class) {
            config.set(extension.toConfig())
            editIdFile.set(editId)
        }
    }

    abstract class Generator @Inject constructor(
            private val fileOps: FileSystemOperations
    ) : WorkAction<Generator.Params> {
        private val file = parameters.editIdFile.get().asFile
        private val appId = file.nameWithoutExtension
        private val publisher = parameters.config.get().credentialStream().use {
            PlayPublisher(it, appId)
        }

        override fun execute() {
            file.safeCreateNewFile().writeText(getOrCreateEditId())
        }

        private fun getOrCreateEditId(): String {
            val editId = file.orNull()?.readText().nullOrFull()?.takeIf {
                file.marked("skipped").exists()
            }
            fileOps.delete { delete(file.editIdAndFriends) }

            val response = if (editId == null) {
                publisher.insertEdit()
            } else {
                file.marked("skipped").safeCreateNewFile()
                publisher.getEdit(editId)
            }

            return when (response) {
                is EditResponse.Success -> response.id
                is EditResponse.Failure -> handleFailure(response)
            }
        }

        private fun handleFailure(response: EditResponse.Failure): String {
            if (response.isNewApp()) {
                // Rethrow for clarity
                response.rethrow("""
                    |No application found for the package name '$appId'. The first version of your
                    |app must be uploaded via the Play Console.
                """.trimMargin())
            } else if (response.isInvalidEdit()) {
                Logging.getLogger(GenerateEdit::class.java)
                        .error("Failed to retrieve saved edit, regenerating.")
                return getOrCreateEditId()
            } else if (response.isUnauthorized()) {
                response.rethrow("""
                    |Service account not authenticated. See the README for instructions:
                    |https://github.com/Triple-T/gradle-play-publisher#service-account
                """.trimMargin())
            } else {
                response.rethrow()
            }
        }

        interface Params : WorkParameters {
            val config: Property<PlayExtensionConfig>
            val editIdFile: RegularFileProperty
        }
    }
}
