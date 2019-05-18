package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.PLUGIN_NAME
import com.github.triplet.gradle.play.internal.has
import com.github.triplet.gradle.play.internal.nullOrFull
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.internal.safeCreateNewFile
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

internal fun AndroidPublisher.getOrCreateEditId(appId: String, savedEditId: File): String = try {
    val editId = savedEditId.orNull()?.readText().nullOrFull()
    if (editId == null) {
        edits().insert(appId, null).execute().id
    } else {
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
            println("Failed to retrieve saved edit.")
            savedEditId.delete()

            getOrCreateEditId(appId, savedEditId)
        }
        e.statusCode == 401 -> throw IllegalArgumentException(
                "Service account not authenticated. See the README for instructions: " +
                        "https://github.com/Triple-T/gradle-play-publisher/" +
                        "blob/master/README.md#service-account", e)
        else -> throw e
    }
}

internal fun AndroidPublisher.commit(
        extension: PlayPublisherExtension,
        appId: String,
        editId: String,
        savedEditId: File
) {
    if (extension.commit) {
        println("Committing changes")
        try {
            edits().commit(appId, editId).execute()
        } finally {
            savedEditId.delete()
        }
    } else {
        println("Changes pending commit")
        savedEditId.safeCreateNewFile().writeText(editId)
    }
}

internal fun PlayPublisherExtension.buildPublisher(): AndroidPublisher {
    val transport = buildTransport()
    val creds = _serviceAccountCredentials!!
    val serviceAccountEmail = _serviceAccountEmail
    val factory = JacksonFactory.getDefaultInstance()

    val credential = if (serviceAccountEmail == null) {
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

    return AndroidPublisher.Builder(transport, JacksonFactory.getDefaultInstance()) {
        credential.initialize(it.setReadTimeout(0))
    }.setApplicationName(PLUGIN_NAME).build()
}

private fun buildTransport(): NetHttpTransport {
    val trustStore: String? = System.getProperty("javax.net.ssl.trustStore", null)
    val trustStorePassword: String? =
            System.getProperty("javax.net.ssl.trustStorePassword", null)

    return if (trustStore == null) {
        GoogleNetHttpTransport.newTrustedTransport()
    } else {
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        FileInputStream(trustStore).use { fis ->
            ks.load(fis, trustStorePassword?.toCharArray())
        }
        NetHttpTransport.Builder().trustCertificates(ks).build()
    }
}
