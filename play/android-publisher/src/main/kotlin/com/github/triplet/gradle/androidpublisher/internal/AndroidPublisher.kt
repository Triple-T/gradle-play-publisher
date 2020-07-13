package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.common.utils.PLUGIN_NAME
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.apache.v2.ApacheHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.ProxyAuthenticationStrategy
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore

internal fun createPublisher(credentials: InputStream): AndroidPublisher {
    val transport = buildTransport()
    val credential = GoogleCredentials.fromStream(credentials) { transport }
            .createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))

    return AndroidPublisher.Builder(
            transport,
            JacksonFactory.getDefaultInstance(),
            AndroidPublisherAdapter(credential)
    ).setApplicationName(PLUGIN_NAME).build()
}

internal infix fun GoogleJsonResponseException.has(error: String) =
        details?.errors.orEmpty().any { it.reason == error }

private fun buildTransport(): HttpTransport {
    val trustStore: String? = System.getProperty("javax.net.ssl.trustStore", null)
    val trustStorePassword: String? =
            System.getProperty("javax.net.ssl.trustStorePassword", null)

    return if (trustStore == null) {
        createHttpTransport()
    } else {
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        FileInputStream(trustStore).use { fis ->
            ks.load(fis, trustStorePassword?.toCharArray())
        }
        NetHttpTransport.Builder().trustCertificates(ks).build()
    }
}

private fun createHttpTransport() : HttpTransport {
    val protocols = arrayOf("http", "https")
    for (protocol in protocols) {
        val proxyHost = System.getProperty("$protocol.proxyHost")
        val proxyUser = System.getProperty("$protocol.proxyUser")
        val proxyPassword = System.getProperty("$protocol.proxyPassword");
        if (proxyHost != null && proxyUser != null && proxyPassword != null) {
            val defaultProxyPort = if (protocol == "http") "80" else "443"
            val proxyPort = Integer.parseInt(System.getProperty("$protocol.proxyPort", defaultProxyPort))
            val credentials = BasicCredentialsProvider()
            credentials.setCredentials(
                    AuthScope(proxyHost, proxyPort),
                    UsernamePasswordCredentials(proxyUser, proxyPassword)
            )
            val httpClient = ApacheHttpTransport.newDefaultHttpClientBuilder()
                    .setProxyAuthenticationStrategy(ProxyAuthenticationStrategy.INSTANCE)
                    .setDefaultCredentialsProvider(credentials)
                    .build()
            return ApacheHttpTransport(httpClient)
        }
    }
    return GoogleNetHttpTransport.newTrustedTransport()
}

private class AndroidPublisherAdapter(
        credential: GoogleCredentials
) : HttpCredentialsAdapter(credential) {
    override fun initialize(request: HttpRequest) {
        super.initialize(request.setReadTimeout(0))
    }
}
