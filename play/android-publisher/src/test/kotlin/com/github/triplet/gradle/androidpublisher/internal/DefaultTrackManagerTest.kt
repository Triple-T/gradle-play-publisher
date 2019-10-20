package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class DefaultTrackManagerTest {
    private val mockPublisher = mock(InternalPlayPublisher::class.java)
    private val tracks = DefaultTrackManager(mockPublisher, "edit-id")

    @Test
    fun `Standard build with completed release creates new track`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.COMPLETED,
                userFraction = .88,
                releaseNotes = mapOf("locale" to "notes"),
                retainableArtifacts = listOf(777),
                releaseName = "relname",
                didPreviousBuildSkipCommit = false
        )

        tracks.update(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.track).isEqualTo("alpha")
        assertThat(trackCaptor.value.releases).hasSize(1)
        assertThat(trackCaptor.value.releases.single().name).isEqualTo("relname")
        assertThat(trackCaptor.value.releases.single().status).isEqualTo("completed")
        assertThat(trackCaptor.value.releases.single().versionCodes).containsExactly(888L, 777L)
        assertThat(trackCaptor.value.releases.single().userFraction).isNull()
        assertThat(trackCaptor.value.releases.single().releaseNotes).hasSize(1)
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().language)
                .isEqualTo("locale")
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().text)
                .isEqualTo("notes")
    }

    @Test
    fun `Standard build with rollout release creates new release`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.IN_PROGRESS,
                userFraction = .88,
                releaseNotes = mapOf("lang1" to "notes1"),
                retainableArtifacts = listOf(777),
                releaseName = "relname",
                didPreviousBuildSkipCommit = false
        )
        `when`(mockPublisher.getTrack(any(), any())).thenReturn(Track().apply {
            track = "alpha"
            releases = listOf(TrackRelease().apply {
                status = "completed"
                versionCodes = listOf(666)
            })
        })

        tracks.update(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.track).isEqualTo("alpha")
        assertThat(trackCaptor.value.releases).hasSize(2)

        assertThat(trackCaptor.value.releases.first().status).isEqualTo("completed")
        assertThat(trackCaptor.value.releases.first().versionCodes).containsExactly(666L)

        assertThat(trackCaptor.value.releases.last().name).isEqualTo("relname")
        assertThat(trackCaptor.value.releases.last().status).isEqualTo("inProgress")
        assertThat(trackCaptor.value.releases.last().versionCodes).containsExactly(888L, 777L)
        assertThat(trackCaptor.value.releases.last().userFraction).isEqualTo(.88)
        assertThat(trackCaptor.value.releases.last().releaseNotes).hasSize(1)
        assertThat(trackCaptor.value.releases.last().releaseNotes.single().language)
                .isEqualTo("lang1")
        assertThat(trackCaptor.value.releases.last().releaseNotes.single().text)
                .isEqualTo("notes1")
    }

    @Test
    fun `Standard build with rollout release overwrites existing rollout release`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.IN_PROGRESS,
                userFraction = .88,
                releaseNotes = mapOf("lang1" to "notes1"),
                retainableArtifacts = listOf(777),
                releaseName = "relname",
                didPreviousBuildSkipCommit = false
        )
        `when`(mockPublisher.getTrack(any(), any())).thenReturn(Track().apply {
            track = "alpha"
            releases = listOf(
                    TrackRelease().apply {
                        status = "inProgress"
                        versionCodes = listOf(666)
                    },
                    TrackRelease().apply {
                        status = "completed"
                        versionCodes = listOf(555)
                    }
            )
        })

        tracks.update(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.track).isEqualTo("alpha")
        assertThat(trackCaptor.value.releases).hasSize(2)

        assertThat(trackCaptor.value.releases.first().status).isEqualTo("completed")
        assertThat(trackCaptor.value.releases.first().versionCodes).containsExactly(555L)

        assertThat(trackCaptor.value.releases.last().name).isEqualTo("relname")
        assertThat(trackCaptor.value.releases.last().status).isEqualTo("inProgress")
        assertThat(trackCaptor.value.releases.last().versionCodes).containsExactly(888L, 777L)
        assertThat(trackCaptor.value.releases.last().userFraction).isEqualTo(.88)
        assertThat(trackCaptor.value.releases.last().releaseNotes).hasSize(1)
        assertThat(trackCaptor.value.releases.last().releaseNotes.single().language)
                .isEqualTo("lang1")
        assertThat(trackCaptor.value.releases.last().releaseNotes.single().text)
                .isEqualTo("notes1")
    }

    @Test
    fun `Skipped commit build with completed release creates new track when none already exists`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.COMPLETED,
                userFraction = .88,
                releaseNotes = mapOf("lang1" to "notes1"),
                retainableArtifacts = listOf(777),
                releaseName = "relname",
                didPreviousBuildSkipCommit = true
        )
        `when`(mockPublisher.getTrack(any(), any())).thenReturn(Track().apply { track = "alpha" })

        tracks.update(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.track).isEqualTo("alpha")
        assertThat(trackCaptor.value.releases).hasSize(1)
        assertThat(trackCaptor.value.releases.single().status).isEqualTo("completed")
        assertThat(trackCaptor.value.releases.single().versionCodes).containsExactly(777L, 888L)
        assertThat(trackCaptor.value.releases.single().name).isEqualTo("relname")
        assertThat(trackCaptor.value.releases.single().status).isEqualTo("completed")
        assertThat(trackCaptor.value.releases.single().userFraction).isNull()
        assertThat(trackCaptor.value.releases.single().releaseNotes).hasSize(1)
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().language)
                .isEqualTo("lang1")
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().text)
                .isEqualTo("notes1")
    }

    @Test
    fun `Skipped commit build with completed release updates existing track when found`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.COMPLETED,
                userFraction = .88,
                releaseNotes = mapOf("lang1" to "notes1"),
                retainableArtifacts = listOf(777),
                releaseName = "relname",
                didPreviousBuildSkipCommit = true
        )
        `when`(mockPublisher.getTrack(any(), any())).thenReturn(Track().apply {
            track = "alpha"
            releases = listOf(TrackRelease().apply {
                status = "completed"
                versionCodes = listOf(666)
                releaseNotes = listOf(LocalizedText().apply {
                    language = "lang2"
                    text = "notes2"
                })
            })
        })

        tracks.update(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.track).isEqualTo("alpha")
        assertThat(trackCaptor.value.releases).hasSize(1)
        assertThat(trackCaptor.value.releases.single().status).isEqualTo("completed")
        assertThat(trackCaptor.value.releases.single().versionCodes).containsExactly(666L, 777L, 888L)
        assertThat(trackCaptor.value.releases.single().name).isEqualTo("relname")
        assertThat(trackCaptor.value.releases.single().status).isEqualTo("completed")
        assertThat(trackCaptor.value.releases.single().userFraction).isNull()
        assertThat(trackCaptor.value.releases.single().releaseNotes).hasSize(2)
        assertThat(trackCaptor.value.releases.single().releaseNotes.first().language)
                .isEqualTo("lang1")
        assertThat(trackCaptor.value.releases.single().releaseNotes.first().text)
                .isEqualTo("notes1")
        assertThat(trackCaptor.value.releases.single().releaseNotes.last().language)
                .isEqualTo("lang2")
        assertThat(trackCaptor.value.releases.single().releaseNotes.last().text)
                .isEqualTo("notes2")
    }

    @Test
    fun `Skipped commit build with existing completed release merges release notes`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.COMPLETED,
                userFraction = .88,
                releaseNotes = mapOf("lang1" to "notes1-2"),
                retainableArtifacts = listOf(777),
                releaseName = "relname",
                didPreviousBuildSkipCommit = true
        )
        `when`(mockPublisher.getTrack(any(), any())).thenReturn(Track().apply {
            releases = listOf(TrackRelease().apply {
                status = "completed"
                releaseNotes = listOf(
                        LocalizedText().apply {
                            language = "lang1"
                            text = "notes1-1"
                        },
                        LocalizedText().apply {
                            language = "lang2"
                            text = "notes2"
                        }
                )
            })
        })

        tracks.update(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.releases.single().releaseNotes).hasSize(2)
        assertThat(trackCaptor.value.releases.single().releaseNotes.first().language)
                .isEqualTo("lang1")
        assertThat(trackCaptor.value.releases.single().releaseNotes.first().text)
                .isEqualTo("notes1-2")
        assertThat(trackCaptor.value.releases.single().releaseNotes.last().language)
                .isEqualTo("lang2")
        assertThat(trackCaptor.value.releases.single().releaseNotes.last().text)
                .isEqualTo("notes2")
    }

    @Test
    fun `Skipped commit build with different release statuses creates new release`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.DRAFT,
                userFraction = .88,
                releaseNotes = mapOf("lang1" to "notes1"),
                retainableArtifacts = listOf(777),
                releaseName = "relname",
                didPreviousBuildSkipCommit = true
        )
        `when`(mockPublisher.getTrack(any(), any())).thenReturn(Track().apply {
            track = "alpha"
            releases = listOf(TrackRelease().apply {
                status = "completed"
                versionCodes = listOf(666)
                releaseNotes = listOf(LocalizedText().apply {
                    language = "lang2"
                    text = "notes2"
                })
            })
        })

        tracks.update(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())

        assertThat(trackCaptor.value.track).isEqualTo("alpha")
        assertThat(trackCaptor.value.releases).hasSize(2)

        assertThat(trackCaptor.value.releases.first().status).isEqualTo("completed")
        assertThat(trackCaptor.value.releases.first().versionCodes).containsExactly(666L)
        assertThat(trackCaptor.value.releases.first().releaseNotes).hasSize(1)
        assertThat(trackCaptor.value.releases.first().releaseNotes.single().language)
                .isEqualTo("lang2")
        assertThat(trackCaptor.value.releases.first().releaseNotes.single().text)
                .isEqualTo("notes2")

        assertThat(trackCaptor.value.releases.last().name).isEqualTo("relname")
        assertThat(trackCaptor.value.releases.last().status).isEqualTo("draft")
        assertThat(trackCaptor.value.releases.last().userFraction).isNull()
        assertThat(trackCaptor.value.releases.last().releaseNotes).hasSize(1)
        assertThat(trackCaptor.value.releases.last().releaseNotes.single().language)
                .isEqualTo("lang1")
        assertThat(trackCaptor.value.releases.last().releaseNotes.single().text)
                .isEqualTo("notes1")
    }
}
