package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.EditManager
import com.github.triplet.gradle.androidpublisher.ReleaseNote
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.google.api.client.googleapis.testing.json.GoogleJsonResponseExceptionFactoryTesting
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.Image
import com.google.api.services.androidpublisher.model.Listing
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    private var edits: EditManager = DefaultEditManager(mockPublisher, mockTracks, "edit-id")

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
                updatePriority = 3,
                retainableArtifacts = listOf(777)
        )

        verify(mockTracks).update(TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888L),
                didPreviousBuildSkipCommit = false,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        releaseName = "relname",
                        releaseNotes = mapOf("locale" to "notes"),
                        userFraction = .88,
                        updatePriority = 3,
                        retainableArtifacts = listOf(777)
                )
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
                updatePriority = 3,
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
                updatePriority = 3,
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
                updatePriority = 3,
                retainableArtifacts = listOf(777)
        )

        verify(mockPublisher).uploadDeobfuscationFile(eq("edit-id"), eq(mockFile), eq(888))
    }

    @Test
    fun `uploadBundle fails silently when conflict occurs with ignore strategy`() {
        `when`(mockPublisher.uploadBundle(any(), any())).thenThrow(
                GoogleJsonResponseExceptionFactoryTesting.newMock(
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
                updatePriority = 3,
                retainableArtifacts = listOf(777)
        )

        verify(mockTracks, never()).update(any())
    }

    @Test
    fun `uploadBundle fails when conflict occurs with fail strategy`() {
        `when`(mockPublisher.uploadBundle(any(), any())).thenThrow(
                GoogleJsonResponseExceptionFactoryTesting.newMock(
                        JacksonFactory.getDefaultInstance(), 400, "apkUpgradeVersionConflict"))

        assertThrows<IllegalStateException> {
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
                    updatePriority = 3,
                    retainableArtifacts = listOf(777)
            )
        }
    }

    @Test
    fun `uploadApk doesn't update tracks`() {
        `when`(mockPublisher.uploadApk(any(), any())).thenReturn(Apk().apply {
            versionCode = 888
        })

        val versionCode = edits.uploadApk(
                apkFile = mockFile,
                mappingFile = null,
                strategy = ResolutionStrategy.FAIL,
                versionCode = 789,
                variantName = "release",
                mainObbRetainable = 123,
                patchObbRetainable = 321
        )

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
        `when`(mockPublisher.uploadApk(any(), any())).thenThrow(
                GoogleJsonResponseExceptionFactoryTesting.newMock(
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
        `when`(mockPublisher.uploadApk(any(), any())).thenThrow(
                GoogleJsonResponseExceptionFactoryTesting.newMock(
                        JacksonFactory.getDefaultInstance(), 400, "apkUpgradeVersionConflict"))

        assertThrows<IllegalStateException> {
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
                updatePriority = 3,
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
                updatePriority = 3,
                retainableArtifacts = listOf(777)
        )

        verify(mockTracks).update(TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888L),
                didPreviousBuildSkipCommit = false,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        releaseName = "relname",
                        releaseNotes = mapOf("locale" to "notes"),
                        userFraction = .88,
                        updatePriority = 3,
                        retainableArtifacts = listOf(777)
                )
        ))
    }

    @Test
    fun `promoteRelease forwards config to track manager`() {
        edits.promoteRelease(
                promoteTrackName = "alpha",
                fromTrackName = "internal",
                releaseStatus = ReleaseStatus.COMPLETED,
                releaseName = "relname",
                releaseNotes = mapOf("locale" to "notes"),
                userFraction = .88,
                updatePriority = 3,
                retainableArtifacts = listOf(777)
        )

        verify(mockTracks).promote(TrackManager.PromoteConfig(
                promoteTrackName = "alpha",
                fromTrackName = "internal",
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        releaseName = "relname",
                        releaseNotes = mapOf("locale" to "notes"),
                        userFraction = .88,
                        updatePriority = 3,
                        retainableArtifacts = listOf(777)
                )
        ))
    }

    @Test
    fun `findMaxAppVersionCode returns 1 on empty tracks`() {
        `when`(mockTracks.findHighestTrack()).thenReturn(null)

        val max = edits.findMaxAppVersionCode()

        assertThat(max).isEqualTo(1)
    }

    @Test
    fun `findMaxAppVersionCode returns 1 on null releases`() {
        `when`(mockTracks.findHighestTrack()).thenReturn(Track())

        val max = edits.findMaxAppVersionCode()

        assertThat(max).isEqualTo(1)
    }

    @Test
    fun `findMaxAppVersionCode succeeds with single track, single release, singe version code`() {
        `when`(mockTracks.findHighestTrack()).thenReturn(Track().apply {
            releases = listOf(
                    TrackRelease().apply {
                        versionCodes = listOf(5)
                    }
            )
        })

        val max = edits.findMaxAppVersionCode()

        assertThat(max).isEqualTo(5)
    }

    @Test
    fun `findMaxAppVersionCode succeeds with single track, single release, multi version code`() {
        `when`(mockTracks.findHighestTrack()).thenReturn(Track().apply {
            releases = listOf(
                    TrackRelease().apply {
                        versionCodes = listOf(5, 4, 8, 7)
                    }
            )
        })

        val max = edits.findMaxAppVersionCode()

        assertThat(max).isEqualTo(8)
    }

    @Test
    fun `findMaxAppVersionCode succeeds with single track, multi release, multi version code`() {
        `when`(mockTracks.findHighestTrack()).thenReturn(Track().apply {
            releases = listOf(
                    TrackRelease().apply {
                        versionCodes = listOf(5, 4, 8, 7)
                    },
                    TrackRelease().apply {
                        versionCodes = listOf(85, 7, 36, 5)
                    }
            )
        })

        val max = edits.findMaxAppVersionCode()

        assertThat(max).isEqualTo(85)
    }

    @Test
    fun `findLeastStableTrackName returns null on null track`() {
        `when`(mockTracks.findHighestTrack()).thenReturn(null)

        val highest = edits.findLeastStableTrackName()

        assertThat(highest).isNull()
    }

    @Test
    fun `findLeastStableTrackName returns track name`() {
        `when`(mockTracks.findHighestTrack()).thenReturn(Track().apply { track = "internal" })

        val highest = edits.findLeastStableTrackName()

        assertThat(highest).isEqualTo("internal")
    }

    @Test
    fun `getReleaseNotes returns data from publisher`() {
        `when`(mockTracks.getReleaseNotes()).thenReturn(mapOf(
                "alpha" to listOf(LocalizedText().apply {
                    language = "en-US"
                    text = "foobar"
                }),
                "production" to listOf(
                        LocalizedText().apply {
                            language = "en-US"
                            text = "prod foobar"
                        },
                        LocalizedText().apply {
                            language = "fr-FR"
                            text = "french foobar"
                        }
                )
        ))

        val notes = edits.getReleaseNotes()

        assertThat(notes).containsExactly(
                ReleaseNote("alpha", "en-US", "foobar"),
                ReleaseNote("production", "en-US", "prod foobar"),
                ReleaseNote("production", "fr-FR", "french foobar")
        )
    }

    @Test
    fun `getImages returns data from publisher`() {
        `when`(mockPublisher.getImages(any(), any(), any())).thenReturn(listOf(
                Image().setSha256("a").setUrl("ha"),
                Image().setSha256("b").setUrl("hb"),
                Image().setSha256("c").setUrl("hc")
        ))

        val result = edits.getImages("en-US", "phoneScreenshots")

        assertThat(result.map { it.sha256 }).containsExactly("a", "b", "c")
        assertThat(result.map { it.url }).containsExactly("ha=h16383", "hb=h16383", "hc=h16383")
    }

    @Test
    fun `publishAppDetails forwards data to publisher`() {
        edits.publishAppDetails("lang", "email", "phone", "website")

        verify(mockPublisher).updateDetails(eq("edit-id"), eq(AppDetails().apply {
            defaultLanguage = "lang"
            contactEmail = "email"
            contactPhone = "phone"
            contactWebsite = "website"
        }))
    }

    @Test
    fun `publishListing forwards data to publisher`() {
        edits.publishListing("lang", "title", "short", "full", "url")

        verify(mockPublisher).updateListing(eq("edit-id"), eq("lang"), eq(Listing().apply {
            title = "title"
            shortDescription = "short"
            fullDescription = "full"
            video = "url"
        }))
    }

    @Test
    fun `publishImages forwards data to publisher`() {
        edits.publishImages("lang", "phoneScreenshots", listOf(mockFile))

        verify(mockPublisher).deleteImages(eq("edit-id"), eq("lang"), eq("phoneScreenshots"))
        verify(mockPublisher).uploadImage(
                eq("edit-id"), eq("lang"), eq("phoneScreenshots"), eq(mockFile))
    }
}
