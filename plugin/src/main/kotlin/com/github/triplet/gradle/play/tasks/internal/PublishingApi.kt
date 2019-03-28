package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.PLUGIN_NAME
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import java.io.FileInputStream
import java.security.KeyStore

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
        credential.initialize(it.apply { readTimeout = 0 })
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
