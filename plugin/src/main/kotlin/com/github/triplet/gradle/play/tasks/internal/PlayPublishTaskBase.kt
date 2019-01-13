package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.PLUGIN_NAME
import com.github.triplet.gradle.play.internal.has
import com.github.triplet.gradle.play.internal.requireCreds
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
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor

abstract class PlayPublishTaskBase : DefaultTask(), ExtensionOptions {
    @get:Nested override lateinit var extension: PlayPublisherExtension
    @get:Internal internal lateinit var variant: ApplicationVariant

    protected val workerExecutor = project.serviceOf<WorkerExecutor>()
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
                readTimeout = 300_000
                connectTimeout = 300_000
            })
        }.setApplicationName(PLUGIN_NAME).build()
    }

    protected fun read(
            skipIfNotFound: Boolean = false,
            block: AndroidPublisher.Edits.(editId: String) -> Unit
    ) {
        val edits = publisher.edits()
        val request = edits.insert(variant.applicationId, null)

        val id = try {
            request.execute().id
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
            } else if (e.statusCode == 401) {
                throw IllegalArgumentException(
                        "Service account not authenticated. See the README for instructions: " +
                                "https://github.com/Triple-T/gradle-play-publisher/" +
                                "blob/master/README.md#service-account", e)
            } else {
                throw e
            }
        }

        edits.block(id)
    }

    protected fun write(block: AndroidPublisher.Edits.(editId: String) -> Unit) = read {
        block(it)
        commit(variant.applicationId, it).execute()
    }
}
