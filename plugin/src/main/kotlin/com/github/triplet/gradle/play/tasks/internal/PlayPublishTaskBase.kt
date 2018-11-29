package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.EDIT_ID_FILE
import com.github.triplet.gradle.play.internal.PLUGIN_NAME
import com.github.triplet.gradle.play.internal.has
import com.github.triplet.gradle.play.internal.nullOrFull
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.requireCreds
import com.github.triplet.gradle.play.internal.safeCreateNewFile
import com.github.triplet.gradle.play.internal.transport
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import java.io.File

abstract class PlayPublishTaskBase : DefaultTask(), ExtensionOptions {
    @get:Nested override lateinit var extension: PlayPublisherExtension
    @get:Internal internal lateinit var variant: ApplicationVariant

    private val savedEditId = File(project.rootProject.buildDir, EDIT_ID_FILE)
    protected val hasSavedEdit get() = savedEditId.exists()

    @get:Internal
    protected val progressLogger: ProgressLogger = services[ProgressLoggerFactory::class.java]
            .newOperation(javaClass)

    @get:Internal
    protected val publisher: AndroidPublisher by lazy {
        val credential = extension.run {
            val creds = requireCreds()
            val serviceAccountEmail = serviceAccountEmail
            val factory = JacksonFactory.getDefaultInstance()

            if (serviceAccountEmail == null) {
                GoogleCredential.fromStream(creds.inputStream(), transport, factory)
                        .createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
            } else {
                GoogleCredential.Builder()
                        .setTransport(transport)
                        .setJsonFactory(factory)
                        .setServiceAccountId(serviceAccountEmail)
                        .setServiceAccountPrivateKeyFromP12File(creds)
                        .setServiceAccountScopes(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
                        .build()
            }
        }

        AndroidPublisher.Builder(transport, JacksonFactory.getDefaultInstance()) {
            credential.initialize(it.apply {
                readTimeout = 100_000
                connectTimeout = 100_000
            })
        }.setApplicationName(PLUGIN_NAME).build()
    }

    protected fun read(
            skipIfNotFound: Boolean = false,
            block: AndroidPublisher.Edits.(editId: String) -> Unit
    ) {
        val edits = publisher.edits()
        val id = try {
            savedEditId.orNull()?.readText().nullOrFull()
                    ?: edits.insert(variant.applicationId, null).execute().id
        } catch (e: GoogleJsonResponseException) {
            if (e has "applicationNotFound") {
                if (skipIfNotFound) {
                    return
                } else {
                    // Rethrow for clarity
                    throw IllegalArgumentException(
                            "No application found for the package name ${variant.applicationId}. " +
                                    "The first version of your app must be uploaded via the " +
                                    "Play Store console.", e)
                }
            } else if (e has "editAlreadyCommitted") {
                logger.info("Failed to retrieve saved edit.")
                project.delete(savedEditId)

                return read(skipIfNotFound, block)
            } else if (e.statusCode == 401) {
                throw IllegalArgumentException("Invalid service account credentials.", e)
            } else {
                throw e
            }
        }

        edits.block(id)
    }

    protected fun write(block: AndroidPublisher.Edits.(editId: String) -> Unit) = read {
        block(it)

        if (extension.commit) {
            try {
                commit(variant.applicationId, it).execute()
            } finally {
                project.delete(savedEditId)
            }
        } else {
            savedEditId.safeCreateNewFile().writeText(it)
        }
    }
}
