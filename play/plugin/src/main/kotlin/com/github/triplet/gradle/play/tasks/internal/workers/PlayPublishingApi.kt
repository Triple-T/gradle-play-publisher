package com.github.triplet.gradle.play.tasks.internal.workers

import com.github.triplet.gradle.common.utils.PLUGIN_NAME
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.auth.http.HttpTransportFactory
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import java.io.FileInputStream
import java.security.KeyStore

internal fun PlayPublisherExtension.Config.buildPublisher(): AndroidPublisher {
    val httpTransport = buildHttpTransport()
    val creds = checkNotNull(serviceAccountCredentials) { "No creds specified" }
    val serviceAccountEmail = serviceAccountEmail

    val credential: GoogleCredentials = if (serviceAccountEmail == null) {
        GoogleCredentials.fromStream(creds.inputStream(), httpTransport)
                .createScoped(AndroidPublisherScopes.ANDROIDPUBLISHER)
    } else {
        ServiceAccountCredentials.newBuilder()
                .apply {
                    serviceAccountCredentials = creds
                    clientEmail = serviceAccountEmail
                    httpTransportFactory = httpTransport
                    scopes = listOf(AndroidPublisherScopes.ANDROIDPUBLISHER)
                }
                .build()
    }

    return AndroidPublisher.Builder(
            buildNetTransport(),
            JacksonFactory.getDefaultInstance(),
            HttpRequestInitializer { credential.refresh() }
    ).setApplicationName(PLUGIN_NAME).build()
}

private fun buildHttpTransport(): HttpTransportFactory = HttpTransportFactory {
    buildNetTransport()
}

private fun buildNetTransport(): NetHttpTransport {
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
