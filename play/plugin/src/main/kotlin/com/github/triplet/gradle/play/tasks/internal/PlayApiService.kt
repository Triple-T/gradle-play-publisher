package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.androidpublisher.CommitResponse
import com.github.triplet.gradle.androidpublisher.EditManager
import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.inject.Inject

internal abstract class PlayApiService @Inject constructor(
        private val fileOps: FileSystemOperations,
) : BuildService<PlayApiService.Params>, OperationCompletionListener, AutoCloseable {
    val publisher by lazy {
        val useAppDefaultCreds = parameters.useApplicationDefaultCredentials.getOrElse(false)
        val useExplicitCreds = (parameters.credentials.asFile.orNull != null ||
                System.getenv(PlayPublisher.CREDENTIAL_ENV_VAR) != null)

        if (useAppDefaultCreds && useExplicitCreds) {
            error("""
                |Cannot use both application default credentials and explicit credentials.
                |Please read our docs for more details:
                |https://github.com/Triple-T/gradle-play-publisher#authenticating-gradle-play-publisher
            """.trimMargin())
        }

        if (useAppDefaultCreds) {
            PlayPublisher(parameters.appId.get(), parameters.impersonateServiceAccount.getOrNull())
        } else {
            credentialStream().use {
                PlayPublisher(it, parameters.appId.get())
            }
        }
    }
    val edits by lazy {
        val editId = getOrCreateEditId()
        editIdFile.safeCreateNewFile().writeText(editId)
        EditManager(publisher, editId)
    }

    private val editId get() = editIdFile.readText()
    private val editIdFile = parameters.editIdFile.get().asFile
    private val editIdFileAndFriends
        get() = listOf(editIdFile, editIdFile.marked("commit"), editIdFile.marked("skipped"))

    var buildFailed = false
        private set

    fun scheduleCommit() {
        editIdFile.marked("commit").safeCreateNewFile()
    }

    val shouldCommit get() = editIdFile.marked("commit").exists()

    fun commit() {
        val response = publisher.commitEdit(editId)
        if (response is CommitResponse.Failure) {
            if (response.failedToSendForReview()) {
                val retryResponse = publisher.commitEdit(editId, sendChangesForReview = false)
                (retryResponse as? CommitResponse.Failure)?.rethrow(response)
            } else {
                response.rethrow()
            }
        }
    }

    fun skipCommit() {
        editIdFile.marked("skipped").safeCreateNewFile()
    }

    val shouldSkip get() = editIdFile.marked("skipped").exists()

    fun validate() {
        publisher.validateEdit(editId)
    }

    fun cleanup() {
        fileOps.delete { delete(editIdFileAndFriends) }
    }

    override fun close() {
        if (shouldCommit) {
            cleanup()
        }
    }

    override fun onFinish(event: FinishEvent) {
        buildFailed = buildFailed || event.result is TaskFailureResult
    }

    private fun getOrCreateEditId(): String {
        val editId = editIdFile.orNull()?.readText().nullOrFull()?.takeIf {
            editIdFile.marked("skipped").exists()
        }
        cleanup()

        val response = if (editId == null) {
            publisher.insertEdit()
        } else {
            editIdFile.marked("skipped").safeCreateNewFile()
            publisher.getEdit(editId)
        }

        return when (response) {
            is EditResponse.Success -> response.id
            is EditResponse.Failure -> handleFailure(response)
        }
    }

    private fun handleFailure(response: EditResponse.Failure): String {
        val appId = parameters.appId
        if (response.isNewApp()) {
            // Rethrow for clarity
            response.rethrow("""
                    |No application found for the package name '$appId'. The first version of your
                    |app must be uploaded via the Play Console.
                """.trimMargin())
        } else if (response.isInvalidEdit()) {
            Logging.getLogger(PlayApiService::class.java)
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

    private fun credentialStream(): InputStream {
        val credsFile = parameters.credentials.asFile.orNull
        if (credsFile != null) {
            return credsFile.inputStream()
        }

        val credsString = System.getenv(PlayPublisher.CREDENTIAL_ENV_VAR)
        if (credsString != null) {
            return ByteArrayInputStream(credsString.toByteArray())
        }

        error("""
            |No credentials specified. Please read our docs for more details:
            |https://github.com/Triple-T/gradle-play-publisher#authenticating-gradle-play-publisher
        """.trimMargin())
    }

    interface Params : BuildServiceParameters {
        val appId: Property<String>
        val credentials: RegularFileProperty
        val useApplicationDefaultCredentials: Property<Boolean>
        val impersonateServiceAccount: Property<String>
        val editIdFile: RegularFileProperty

        @Suppress("PropertyName") // Don't use this
        val _extensionPriority: Property<Int>
    }
}
