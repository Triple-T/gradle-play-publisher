package com.github.triplet.gradle.androidpublisher.internal

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpResponseException
import com.google.api.client.http.LowLevelHttpRequest
import com.google.api.client.http.LowLevelHttpResponse
import com.google.api.client.testing.http.MockHttpTransport
import com.google.api.client.testing.http.MockLowLevelHttpRequest
import com.google.api.client.testing.http.MockLowLevelHttpResponse
import com.google.api.client.util.Sleeper
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

class AndroidPublisherAdapterTest {
    private val credentials = GoogleCredentials.create(AccessToken("token", null))
    private val adapter = AndroidPublisherAdapter(credentials, Sleeper { })

    @Test
    fun `Server errors are retried`() {
        val responses = ArrayDeque(listOf(503, 500, 200))
        val transport = transportOf { MockLowLevelHttpResponse().setStatusCode(responses.removeFirst()) }

        val response = execute(transport)

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(responses).isEmpty()
    }

    @Test
    fun `Network errors are retried`() {
        var calls = 0
        val transport = transportOf {
            if (calls++ == 0) throw IOException("Connection reset")
            MockLowLevelHttpResponse().setStatusCode(200)
        }

        val response = execute(transport)

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(calls).isEqualTo(2)
    }

    @Test
    fun `Client errors are not retried`() {
        var calls = 0
        val transport = transportOf {
            calls++
            MockLowLevelHttpResponse().setStatusCode(400)
        }

        assertThrows<HttpResponseException> { execute(transport) }
        assertThat(calls).isEqualTo(1)
    }

    private fun execute(transport: MockHttpTransport) = transport.createRequestFactory(adapter)
            .buildGetRequest(GenericUrl("https://androidpublisher.googleapis.com"))
            .execute()

    private fun transportOf(respond: () -> LowLevelHttpResponse) = object : MockHttpTransport() {
        override fun buildRequest(method: String, url: String): LowLevelHttpRequest =
                object : MockLowLevelHttpRequest(url) {
                    override fun execute() = respond()
                }
    }
}
