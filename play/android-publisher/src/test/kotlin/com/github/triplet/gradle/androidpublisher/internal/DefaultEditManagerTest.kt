package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.Json
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.testing.http.HttpTesting
import com.google.api.client.testing.http.MockHttpTransport.Builder
import com.google.api.client.testing.http.MockLowLevelHttpResponse
import com.google.api.services.androidpublisher.model.Bundle
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.io.File

class DefaultEditManagerTest {
    private var mockPublisher = mock(InternalPlayPublisher::class.java)
    private var mockTracks = mock(TrackManager::class.java)
    private var edits = DefaultEditManager(mockPublisher, mockTracks, "edit-id")

    private var mockFile = mock(File::class.java)

    @Test
    fun `uploadBundle forwards config to track manager`() {
        `when`(mockPublisher.uploadBundle(any(), any())).thenReturn(Bundle().apply {
            versionCode = 888
        })
        `when`(mockFile.length()).thenReturn(1)

        edits.uploadBundle(
                bundleFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 888,
                variantName = "release",
                didPreviousBuildSkipCommit = false,
                trackName = "alpha",
                releaseStatus = ReleaseStatus.COMPLETED,
                releaseName = "relname",
                releaseNotes = mapOf("locale" to "notes"),
                userFraction = .88,
                retainableArtifacts = listOf(777)
        )

        verify(mockPublisher).uploadDeobfuscationFile(eq("edit-id"), any(), eq(888))
        verify(mockTracks).update(TrackManager.UpdateConfig(
                versionCodes = listOf(888L),
                didPreviousBuildSkipCommit = false,
                trackName = "alpha",
                releaseStatus = ReleaseStatus.COMPLETED,
                releaseName = "relname",
                releaseNotes = mapOf("locale" to "notes"),
                userFraction = .88,
                retainableArtifacts = listOf(777)
        ))
    }

    @Test
    fun `uploadBundle skips mapping file upload when null`() {
        `when`(mockPublisher.uploadBundle(any(), any())).thenReturn(Bundle().apply {
            versionCode = 888
        })

        edits.uploadBundle(
                bundleFile = mockFile,
                mappingFile = null,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 888,
                variantName = "release",
                didPreviousBuildSkipCommit = false,
                trackName = "alpha",
                releaseStatus = ReleaseStatus.COMPLETED,
                releaseName = "relname",
                releaseNotes = mapOf("locale" to "notes"),
                userFraction = .88,
                retainableArtifacts = listOf(777)
        )

        verify(mockPublisher, never()).uploadDeobfuscationFile(any(), any(), anyInt())
    }

    @Test
    fun `uploadBundle skips mapping file upload when empty`() {
        `when`(mockPublisher.uploadBundle(any(), any())).thenReturn(Bundle().apply {
            versionCode = 888
        })
        `when`(mockFile.length()).thenReturn(0)

        edits.uploadBundle(
                bundleFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 888,
                variantName = "release",
                didPreviousBuildSkipCommit = false,
                trackName = "alpha",
                releaseStatus = ReleaseStatus.COMPLETED,
                releaseName = "relname",
                releaseNotes = mapOf("locale" to "notes"),
                userFraction = .88,
                retainableArtifacts = listOf(777)
        )

        verify(mockPublisher, never()).uploadDeobfuscationFile(any(), any(), anyInt())
    }

    @Test
    fun `uploadBundle uploads mapping file when non-empty`() {
        `when`(mockPublisher.uploadBundle(any(), any())).thenReturn(Bundle().apply {
            versionCode = 888
        })
        `when`(mockFile.length()).thenReturn(1)

        edits.uploadBundle(
                bundleFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 888,
                variantName = "release",
                didPreviousBuildSkipCommit = false,
                trackName = "alpha",
                releaseStatus = ReleaseStatus.COMPLETED,
                releaseName = "relname",
                releaseNotes = mapOf("locale" to "notes"),
                userFraction = .88,
                retainableArtifacts = listOf(777)
        )

        verify(mockPublisher).uploadDeobfuscationFile(eq("edit-id"), eq(mockFile), eq(888))
    }

    @Test
    fun `uploadBundle fails silently when conflict occurs with ignore strategy`() {
        `when`(mockPublisher.uploadBundle(any(), any())).thenThrow(newExceptionMock(
                JacksonFactory.getDefaultInstance(), 400, "apkUpgradeVersionConflict"))

        edits.uploadBundle(
                bundleFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.IGNORE,
                versionCode = 888,
                variantName = "release",
                didPreviousBuildSkipCommit = false,
                trackName = "alpha",
                releaseStatus = ReleaseStatus.COMPLETED,
                releaseName = "relname",
                releaseNotes = mapOf("locale" to "notes"),
                userFraction = .88,
                retainableArtifacts = listOf(777)
        )

        verify(mockTracks, never()).update(any())
    }

    @Test
    fun `uploadBundle fails when conflict occurs with fail strategy`() {
        `when`(mockPublisher.uploadBundle(any(), any())).thenThrow(newExceptionMock(
                JacksonFactory.getDefaultInstance(), 400, "apkUpgradeVersionConflict"))

        assertThrows(Exception::class.java) {
            edits.uploadBundle(
                    bundleFile = mockFile,
                    mappingFile = mockFile,
                    strategy = ResolutionStrategy.FAIL,
                    versionCode = 888,
                    variantName = "release",
                    didPreviousBuildSkipCommit = false,
                    trackName = "alpha",
                    releaseStatus = ReleaseStatus.COMPLETED,
                    releaseName = "relname",
                    releaseNotes = mapOf("locale" to "notes"),
                    userFraction = .88,
                    retainableArtifacts = listOf(777)
            )
        }
    }

    private fun newExceptionMock(
            jsonFactory: JsonFactory,
            httpCode: Int,
            reasonPhrase: String
    ): GoogleJsonResponseException {
        val otherServiceUnavaiableLowLevelResponse = MockLowLevelHttpResponse()
                .setStatusCode(httpCode)
                .setReasonPhrase(reasonPhrase)
                .setContentType(Json.MEDIA_TYPE)
                .setContent("{ \"error\": { \"errors\": [ { \"reason\": \"$reasonPhrase\" } ], " +
                                    "\"code\": $httpCode } }")
        val otherTransport = Builder()
                .setLowLevelHttpResponse(otherServiceUnavaiableLowLevelResponse)
                .build()
        val otherRequest = otherTransport
                .createRequestFactory().buildGetRequest(HttpTesting.SIMPLE_GENERIC_URL)
        otherRequest.throwExceptionOnExecuteError = false
        val otherServiceUnavailableResponse = otherRequest.execute()
        return GoogleJsonResponseException.from(jsonFactory, otherServiceUnavailableResponse)
    }
}
