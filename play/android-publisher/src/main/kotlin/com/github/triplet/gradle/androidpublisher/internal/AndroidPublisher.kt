package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.common.utils.PLUGIN_NAME
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore

internal data class ServiceAccountAuth(
        val credentials: InputStream,
        val email: String?
)

internal fun createPublisher(auth: ServiceAccountAuth): AndroidPublisher {
    val transport = buildTransport()
    val factory = JacksonFactory.getDefaultInstance()

    val credential = if (auth.email == null) {
        GoogleCredential.fromStream(auth.credentials, transport, factory)
                .createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
    } else {
        GoogleCredential.Builder()
                .setTransport(transport)
                .setJsonFactory(factory)
                .setServiceAccountId(auth.email)
                .setServiceAccountPrivateKeyFromP12File(auth.credentials)
                .setServiceAccountScopes(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
                .build()
    }

    return AndroidPublisher.Builder(transport, JacksonFactory.getDefaultInstance()) {
        credential.initialize(it.setReadTimeout(0))
    }.setApplicationName(PLUGIN_NAME).build()
}

internal infix fun GoogleJsonResponseException.has(error: String) =
        details?.errors.orEmpty().any { it.reason == error }

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
