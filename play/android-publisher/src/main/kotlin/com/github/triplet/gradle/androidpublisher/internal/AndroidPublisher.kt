package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.common.utils.PLUGIN_NAME
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.apache.v2.ApacheHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ImpersonatedCredentials
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.ProxyAuthenticationStrategy
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore
import java.util.concurrent.TimeUnit

internal fun createPublisher(credentials: InputStream): AndroidPublisher {
    val transport = buildTransport()
    val credential = try {
        GoogleCredentials.fromStream(credentials) { transport }
    } catch (e: Exception) {
        throw Exception(
                "Credential parsing may have failed. " +
                        "Ensure credential files supplied in the DSL contain valid JSON " +
                        "and/or the ANDROID_PUBLISHER_CREDENTIALS envvar contains valid JSON " +
                        "(not a file path).", e)
    }.createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))

    return AndroidPublisher.Builder(
            transport,
            GsonFactory.getDefaultInstance(),
            AndroidPublisherAdapter(credential)
    ).setApplicationName(PLUGIN_NAME).build()
}

internal fun createPublisher(impersonateServiceAccount: String?): AndroidPublisher {
    val transport = buildTransport()

    val appDefaultCreds = GoogleCredentials.getApplicationDefault()

    val credential = if (impersonateServiceAccount != null) {
        ImpersonatedCredentials.newBuilder()
                .setSourceCredentials(appDefaultCreds)
                .setTargetPrincipal(impersonateServiceAccount)
                .setScopes(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
                .setLifetime(300)
                .setDelegates(null)
                .build()
    } else {
        appDefaultCreds
    }

    return AndroidPublisher.Builder(
            transport,
            GsonFactory.getDefaultInstance(),
            AndroidPublisherAdapter(credential as GoogleCredentials)
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

private fun createHttpTransport(): HttpTransport {
    val protocols = arrayOf("http", "https")
    for (protocol in protocols) {
        val proxyHost = System.getProperty("$protocol.proxyHost")
        val proxyUser = System.getProperty("$protocol.proxyUser")
        val proxyPassword = System.getProperty("$protocol.proxyPassword")
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
        credential: GoogleCredentials,
) : HttpCredentialsAdapter(credential) {
    override fun initialize(request: HttpRequest) {
        val backOffHandler = HttpBackOffUnsuccessfulResponseHandler(
                ExponentialBackOff.Builder()
                        .setMaxElapsedTimeMillis(TimeUnit.MINUTES.toMillis(3).toInt())
                        .build()
        )

        super.initialize(
                request.setReadTimeout(0)
                        .setUnsuccessfulResponseHandler(backOffHandler)
        )
    }
}
