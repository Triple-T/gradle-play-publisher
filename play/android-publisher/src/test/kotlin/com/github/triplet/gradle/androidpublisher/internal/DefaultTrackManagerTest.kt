package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class DefaultTrackManagerTest {
    private val mockPublisher = mock(InternalPlayPublisher::class.java)
    private val tracks: TrackManager = DefaultTrackManager(mockPublisher, "edit-id")

    @Test
    fun `Standard build with completed release creates new track`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.COMPLETED,
                didPreviousBuildSkipCommit = false,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
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
                .isEqualTo("lang1")
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().text)
                .isEqualTo("notes1")
    }

    @Test
    fun `Standard build with rollout release creates new release`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.IN_PROGRESS,
                didPreviousBuildSkipCommit = false,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.IN_PROGRESS,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
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
                didPreviousBuildSkipCommit = false,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.IN_PROGRESS,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
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
                didPreviousBuildSkipCommit = true,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
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
                didPreviousBuildSkipCommit = true,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
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
                didPreviousBuildSkipCommit = true,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1-2"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
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
        assertThat(trackCaptor.value.releases).hasSize(1)
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
    fun `Skipped commit build with existing params keeps them if no new ones are specified`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.DRAFT,
                didPreviousBuildSkipCommit = true,
                base = TrackManager.BaseConfig(
                        releaseStatus = null,
                        userFraction = null,
                        releaseNotes = emptyMap(),
                        retainableArtifacts = null,
                        releaseName = null
                )
        )
        `when`(mockPublisher.getTrack(any(), any())).thenReturn(Track().apply {
            releases = listOf(TrackRelease().apply {
                status = "draft"
                name = "foobar"
                userFraction = 789.0
                versionCodes = listOf(3, 4, 5)
                releaseNotes = listOf(
                        LocalizedText().apply {
                            language = "lang1"
                            text = "notes1"
                        }
                )
            })
        })

        tracks.update(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.releases).hasSize(1)
        assertThat(trackCaptor.value.releases.single().name).isEqualTo("foobar")
        assertThat(trackCaptor.value.releases.single().status).isEqualTo("draft")
        assertThat(trackCaptor.value.releases.single().userFraction).isEqualTo(789.0)
        assertThat(trackCaptor.value.releases.single().versionCodes).containsExactly(3L, 4L, 5L, 888L)
        assertThat(trackCaptor.value.releases.single().releaseNotes).hasSize(1)
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().language)
                .isEqualTo("lang1")
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().text)
                .isEqualTo("notes1")
    }

    @Test
    fun `Skipped commit build with different release statuses creates new release`() {
        val config = TrackManager.UpdateConfig(
                trackName = "alpha",
                versionCodes = listOf(888),
                releaseStatus = ReleaseStatus.DRAFT,
                didPreviousBuildSkipCommit = true,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.DRAFT,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
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

    @Test
    fun `findMaxAppVersionCode returns 1 on empty tracks`() {
        `when`(mockPublisher.listTracks(any())).thenReturn(emptyList())

        val max = tracks.findMaxAppVersionCode()

        assertThat(max).isEqualTo(1)
    }

    @Test
    fun `findMaxAppVersionCode returns 1 on null releases`() {
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(Track(), Track()))

        val max = tracks.findMaxAppVersionCode()

        assertThat(max).isEqualTo(1)
    }

    @Test
    fun `findMaxAppVersionCode succeeds with single track, single release, singe version code`() {
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(Track().apply {
            releases = listOf(
                    TrackRelease().apply {
                        versionCodes = listOf(5)
                    }
            )
        }))

        val max = tracks.findMaxAppVersionCode()

        assertThat(max).isEqualTo(5)
    }

    @Test
    fun `findMaxAppVersionCode succeeds with single track, single release, multi version code`() {
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(Track().apply {
            releases = listOf(
                    TrackRelease().apply {
                        versionCodes = listOf(5, 4, 8, 7)
                    }
            )
        }))

        val max = tracks.findMaxAppVersionCode()

        assertThat(max).isEqualTo(8)
    }

    @Test
    fun `findMaxAppVersionCode succeeds with single track, multi release, multi version code`() {
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(Track().apply {
            releases = listOf(
                    TrackRelease().apply {
                        versionCodes = listOf(5, 4, 8, 7)
                    },
                    TrackRelease().apply {
                        versionCodes = listOf(85, 7, 36, 5)
                    }
            )
        }))

        val max = tracks.findMaxAppVersionCode()

        assertThat(max).isEqualTo(85)
    }

    @Test
    fun `findMaxAppVersionCode succeeds with multi track, multi release, multi version code`() {
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(
                Track().apply {
                    releases = listOf(
                            TrackRelease().apply {
                                versionCodes = listOf(5, 4, 8, 7)
                            },
                            TrackRelease().apply {
                                versionCodes = listOf(85, 7, 36, 5)
                            }
                    )
                },
                Track().apply {
                    releases = listOf(
                            TrackRelease().apply {
                                versionCodes = listOf(49, 5875, 385, 9, 73, 294, 867)
                            },
                            TrackRelease().apply {
                                versionCodes = listOf(5, 4, 8, 7)
                            },
                            TrackRelease().apply {
                                versionCodes = listOf(85, 7, 36, 5)
                            }
                    )
                }
        ))

        val max = tracks.findMaxAppVersionCode()

        assertThat(max).isEqualTo(5875)
    }

    @Test
    fun `Promoting tracks with no active releases fails`() {
        val config = TrackManager.PromoteConfig(
                promoteTrackName = "alpha",
                fromTrackName = null,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
        )
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(
                Track(),
                Track().apply {
                    releases = listOf(TrackRelease())
                }
        ))

        assertThrows(IllegalStateException::class.java) {
            tracks.promote(config)
        }

        verify(mockPublisher, never()).updateTrack(any(), any())
    }

    @Test
    fun `Promoting from specific track with no active releases fails`() {
        val config = TrackManager.PromoteConfig(
                promoteTrackName = "alpha",
                fromTrackName = "foobar",
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
        )
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(
                Track().apply {
                    releases = listOf(
                            TrackRelease().apply {
                                versionCodes = listOf(1)
                            }
                    )
                },
                Track().apply {
                    track = "foobar"
                    releases = listOf(TrackRelease())
                }
        ))

        assertThrows(IllegalStateException::class.java) {
            tracks.promote(config)
        }

        verify(mockPublisher, never()).updateTrack(any(), any())
    }

    @Test
    fun `Promoting from non-existent specific track fails`() {
        val config = TrackManager.PromoteConfig(
                promoteTrackName = "alpha",
                fromTrackName = "foobar",
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
        )
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(Track().apply {
            track = "abcd"
            releases = listOf(
                    TrackRelease().apply {
                        versionCodes = listOf(1)
                    }
            )
        }))

        assertThrows(IllegalStateException::class.java) {
            tracks.promote(config)
        }

        verify(mockPublisher, never()).updateTrack(any(), any())
    }

    @Test
    fun `Promoting from dynamic track chooses highest available`() {
        val config = TrackManager.PromoteConfig(
                promoteTrackName = "alpha",
                fromTrackName = null,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = null
                )
        )
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(
                Track().apply {
                    releases = listOf(
                            TrackRelease().apply {
                                name = "old"
                                versionCodes = listOf(3)
                            }
                    )
                },
                Track().apply {
                    releases = listOf(
                            TrackRelease().apply {
                                name = "new"
                                versionCodes = listOf(4)
                            }
                    )
                }
        ))

        tracks.promote(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.releases).hasSize(1)
        assertThat(trackCaptor.value.releases.single().name).isEqualTo("new")
    }

    @Test
    fun `Promoting track applies updates from params`() {
        val config = TrackManager.PromoteConfig(
                promoteTrackName = "alpha",
                fromTrackName = null,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.IN_PROGRESS,
                        userFraction = .88,
                        releaseNotes = mapOf("lang1" to "notes1"),
                        retainableArtifacts = listOf(777),
                        releaseName = "relname"
                )
        )
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(Track().apply {
            releases = listOf(
                    TrackRelease().apply {
                        versionCodes = listOf(3)
                    }
            )
        }))

        tracks.promote(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.releases).hasSize(1)
        assertThat(trackCaptor.value.releases.single().versionCodes).containsExactly(3L, 777L)
        assertThat(trackCaptor.value.releases.single().name).isEqualTo("relname")
        assertThat(trackCaptor.value.releases.single().status).isEqualTo("inProgress")
        assertThat(trackCaptor.value.releases.single().userFraction).isEqualTo(.88)
        assertThat(trackCaptor.value.releases.single().releaseNotes).hasSize(1)
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().language)
                .isEqualTo("lang1")
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().text)
                .isEqualTo("notes1")
    }

    @Test
    fun `Promoting track with conflicting releases keeps newest one`() {
        val config = TrackManager.PromoteConfig(
                promoteTrackName = "alpha",
                fromTrackName = null,
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        userFraction = null,
                        releaseNotes = emptyMap(),
                        retainableArtifacts = null,
                        releaseName = null
                )
        )
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(Track().apply {
            releases = listOf(
                    TrackRelease().apply {
                        status = "completed"
                        versionCodes = listOf(4)
                    },
                    TrackRelease().apply {
                        status = "inProgress"
                        versionCodes = listOf(5)
                    },
                    TrackRelease().apply {
                        status = "draft"
                        versionCodes = listOf(3)
                    }
            )
        }))

        tracks.promote(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.releases).hasSize(1)
        assertThat(trackCaptor.value.releases.single().status).isEqualTo("completed")
        assertThat(trackCaptor.value.releases.single().versionCodes).containsExactly(5L)
    }

    @Test
    fun `Promoting track keeps existing values if params aren't specified`() {
        val config = TrackManager.PromoteConfig(
                promoteTrackName = "alpha",
                fromTrackName = null,
                base = TrackManager.BaseConfig(
                        releaseStatus = null,
                        userFraction = null,
                        releaseNotes = emptyMap(),
                        retainableArtifacts = null,
                        releaseName = null
                )
        )
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(Track().apply {
            releases = listOf(TrackRelease().apply {
                status = "completed"
                name = "foobar"
                userFraction = 789.0
                versionCodes = listOf(3, 4, 5)
                releaseNotes = listOf(
                        LocalizedText().apply {
                            language = "lang1"
                            text = "notes1"
                        }
                )
            })
        }))

        tracks.promote(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.releases).hasSize(1)
        assertThat(trackCaptor.value.releases.single().name).isEqualTo("foobar")
        assertThat(trackCaptor.value.releases.single().status).isEqualTo("completed")
        assertThat(trackCaptor.value.releases.single().userFraction).isEqualTo(789.0)
        assertThat(trackCaptor.value.releases.single().versionCodes).containsExactly(3L, 4L, 5L)
        assertThat(trackCaptor.value.releases.single().releaseNotes).hasSize(1)
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().language)
                .isEqualTo("lang1")
        assertThat(trackCaptor.value.releases.single().releaseNotes.single().text)
                .isEqualTo("notes1")
    }

    @Test
    fun `Promoting from different track updates track name`() {
        val config = TrackManager.PromoteConfig(
                promoteTrackName = "alpha",
                fromTrackName = "internal",
                base = TrackManager.BaseConfig(
                        releaseStatus = ReleaseStatus.COMPLETED,
                        userFraction = null,
                        releaseNotes = emptyMap(),
                        retainableArtifacts = null,
                        releaseName = null
                )
        )
        `when`(mockPublisher.listTracks(any())).thenReturn(listOf(
                Track().apply {
                    track = "alpha"
                    releases = listOf(TrackRelease().apply { versionCodes = listOf(1) })
                },
                Track().apply {
                    track = "internal"
                    releases = listOf(TrackRelease().apply { versionCodes = listOf(2) })
                }
        ))

        tracks.promote(config)

        val trackCaptor = ArgumentCaptor.forClass(Track::class.java)
        verify(mockPublisher).updateTrack(eq("edit-id"), trackCaptor.capture())
        assertThat(trackCaptor.value.track).isEqualTo("alpha")
        assertThat(trackCaptor.value.releases).hasSize(1)
        assertThat(trackCaptor.value.releases.single().versionCodes).containsExactly(2L)
    }
}
