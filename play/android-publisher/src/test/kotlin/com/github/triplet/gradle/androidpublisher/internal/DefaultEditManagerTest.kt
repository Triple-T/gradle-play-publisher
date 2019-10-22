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
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.Bundle
import com.google.common.truth.Truth.assertThat
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

    @Test
    fun `uploadApk completes successfully`() {
        `when`(mockPublisher.uploadApk(any(), any())).thenReturn(Apk().apply {
            versionCode = 888
        })
        `when`(mockFile.length()).thenReturn(1)

        val versionCode = edits.uploadApk(
                apkFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 789,
                variantName = "release",
                mainObbRetainable = 123,
                patchObbRetainable = 321
        )

        verify(mockPublisher).uploadDeobfuscationFile(eq("edit-id"), eq(mockFile), eq(888))
        verify(mockTracks, never()).update(any())
        assertThat(versionCode).isEqualTo(888)
    }

    @Test
    fun `uploadApk skips mapping file upload when null`() {
        `when`(mockPublisher.uploadApk(any(), any())).thenReturn(Apk().apply {
            versionCode = 888
        })

        edits.uploadApk(
                apkFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 888,
                variantName = "release",
                mainObbRetainable = 123,
                patchObbRetainable = 321
        )

        verify(mockPublisher, never()).uploadDeobfuscationFile(any(), any(), anyInt())
    }

    @Test
    fun `uploadApk skips mapping file upload when empty`() {
        `when`(mockPublisher.uploadApk(any(), any())).thenReturn(Apk().apply {
            versionCode = 888
        })
        `when`(mockFile.length()).thenReturn(0)

        edits.uploadApk(
                apkFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 888,
                variantName = "release",
                mainObbRetainable = 123,
                patchObbRetainable = 321
        )

        verify(mockPublisher, never()).uploadDeobfuscationFile(any(), any(), anyInt())
    }

    @Test
    fun `uploadApk uploads mapping file when non-empty`() {
        `when`(mockPublisher.uploadApk(any(), any())).thenReturn(Apk().apply {
            versionCode = 888
        })
        `when`(mockFile.length()).thenReturn(1)

        edits.uploadApk(
                apkFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 888,
                variantName = "release",
                mainObbRetainable = 123,
                patchObbRetainable = 321
        )

        verify(mockPublisher).uploadDeobfuscationFile(eq("edit-id"), eq(mockFile), eq(888))
    }

    @Test
    fun `uploadApk fails silently when conflict occurs with ignore strategy`() {
        `when`(mockPublisher.uploadApk(any(), any())).thenThrow(newExceptionMock(
                JacksonFactory.getDefaultInstance(), 400, "apkUpgradeVersionConflict"))

        edits.uploadApk(
                apkFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.IGNORE,
                versionCode = 888,
                variantName = "release",
                mainObbRetainable = 123,
                patchObbRetainable = 321
        )

        verify(mockTracks, never()).update(any())
    }

    @Test
    fun `uploadApk fails when conflict occurs with fail strategy`() {
        `when`(mockPublisher.uploadApk(any(), any())).thenThrow(newExceptionMock(
                JacksonFactory.getDefaultInstance(), 400, "apkUpgradeVersionConflict"))

        assertThrows(Exception::class.java) {
            edits.uploadApk(
                    apkFile = mockFile,
                    mappingFile = mockFile,
                    strategy = ResolutionStrategy.FAIL,
                    versionCode = 888,
                    variantName = "release",
                    mainObbRetainable = 123,
                    patchObbRetainable = 321
            )
        }
    }

    @Test
    fun `uploadApk attaches OBBs when provided`() {
        `when`(mockPublisher.uploadApk(any(), any())).thenReturn(Apk().apply {
            versionCode = 888
        })

        edits.uploadApk(
                apkFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 888,
                variantName = "release",
                mainObbRetainable = 123,
                patchObbRetainable = 321
        )

        verify(mockPublisher).attachObb(eq("edit-id"), eq("main"), eq(888), eq(123))
        verify(mockPublisher).attachObb(eq("edit-id"), eq("patch"), eq(888), eq(321))
    }

    @Test
    fun `uploadApk ignores OBBs when not provided`() {
        `when`(mockPublisher.uploadApk(any(), any())).thenReturn(Apk().apply {
            versionCode = 888
        })

        edits.uploadApk(
                apkFile = mockFile,
                mappingFile = mockFile,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 888,
                variantName = "release",
                mainObbRetainable = null,
                patchObbRetainable = null
        )

        verify(mockPublisher, never()).attachObb(any(), any(), anyInt(), anyInt())
        verify(mockPublisher, never()).attachObb(any(), any(), anyInt(), anyInt())
    }

    @Test
    fun `publishApk ignores empty version codes`() {
        edits.publishApk(
                versionCodes = emptyList(),
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
    fun `publishApk forwards config to track manager`() {
        edits.publishApk(
                versionCodes = listOf(888L),
                didPreviousBuildSkipCommit = false,
                trackName = "alpha",
                releaseStatus = ReleaseStatus.COMPLETED,
                releaseName = "relname",
                releaseNotes = mapOf("locale" to "notes"),
                userFraction = .88,
                retainableArtifacts = listOf(777)
        )

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

    // TODO(asaveau): remove once https://github.com/googleapis/google-api-java-client/pull/1395
    //  goes through
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
