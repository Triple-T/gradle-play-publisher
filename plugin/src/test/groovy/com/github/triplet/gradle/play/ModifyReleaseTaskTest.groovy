package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.TrackType
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.*
import kotlin.LazyKt
import org.gradle.api.Task
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Matchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class ModifyReleaseTaskTest {

    @Mock
    AndroidPublisher publisherMock

    @Mock
    AndroidPublisher.Edits editsMock

    @Mock
    AndroidPublisher.Edits.Insert insertMock

    @Mock
    AndroidPublisher.Edits.Tracks editTracksMock

    @Mock
    AndroidPublisher.Edits.Tracks.List editTracksListMock

    @Mock
    AndroidPublisher.Edits.Commit editCommitMock

    @Mock
    AndroidPublisher.Edits.Tracks.Update tracksUpdateMock

    @Mock
    AndroidPublisher.Edits.Tracks.Get getInternalTrackMock

    @Mock
    AndroidPublisher.Edits.Tracks.Get getAlphaTrackMock

    @Mock
    AndroidPublisher.Edits.Tracks.Get getBetaTrackMock

    @Mock
    AndroidPublisher.Edits.Tracks.Get getProdTrackMock

    @Mock
    AndroidPublisher.Edits.Apks apksMock

    @Mock
    AndroidPublisher.Edits.Apks.Upload uploadMock

    // These are final and not mock able
    final AppEdit appEdit = new AppEdit()
    final Apk apk = new Apk()
    final Track internalTrack = new Track()
            .setTrack(TrackType.INTERNAL.publishedName)
            .setReleases([new TrackRelease().setVersionCodes([100,90])])
    final Track alphaTrack = new Track()
            .setTrack(TrackType.ALPHA.publishedName)
            .setReleases([new TrackRelease().setVersionCodes([80,70])])
    final Track betaTrack = new Track()
            .setTrack(TrackType.BETA.publishedName)
            .setReleases([new TrackRelease().setVersionCodes([70,60])])
    final Track prodTrack = new Track()
            .setTrack(TrackType.PRODUCTION.publishedName)
            .setReleases([new TrackRelease().setVersionCodes([60,50])])
    final List<Track> tracksList = [
            internalTrack,
            alphaTrack,
            betaTrack,
            prodTrack
    ]
    final TracksListResponse tracksListResponse = new TracksListResponse().setTracks(tracksList)

    @Before
    void setup() {
        initMocks(this)

        appEdit.setId('424242')
        apk.setVersionCode(42)

        doReturn(editsMock).when(publisherMock).edits()
        doReturn(insertMock).when(editsMock).insert(anyString(), any(AppEdit.class))
        doReturn(appEdit).when(insertMock).execute()

        doReturn(editTracksListMock).when(editTracksMock).list(anyString(), anyString())
        doReturn(tracksListResponse).when(editTracksListMock).execute()

        doReturn(apksMock).when(editsMock).apks()
        doReturn(uploadMock).when(apksMock).upload(anyString(), anyString(), any(FileContent.class))
        doReturn(apk).when(uploadMock).execute()

        doReturn(editTracksMock).when(editsMock).tracks()

        doReturn(getInternalTrackMock).when(editTracksMock).get(anyString(), anyString(), eq('internal'))
        doReturn(internalTrack).when(getInternalTrackMock).execute()

        doReturn(getAlphaTrackMock).when(editTracksMock).get(anyString(), anyString(), eq('alpha'))
        doReturn(alphaTrack).when(getAlphaTrackMock).execute()

        doReturn(getBetaTrackMock).when(editTracksMock).get(anyString(), anyString(), eq('beta'))
        doReturn(betaTrack).when(getBetaTrackMock).execute()

        doReturn(tracksUpdateMock).when(editTracksMock).update(anyString(), anyString(), anyString(), any(Track.class))
        doReturn(editCommitMock).when(editsMock).commit(anyString(), anyString())
    }

    @Test(expected = IllegalArgumentException)
    void whenModifyTrack_withNoModifiersSpecified_throwsException() {
        def project = TestHelper.evaluatableProject()

        project.evaluate()

        // Attach the mock
        setMockPublisher(project.tasks.modifyRelease)

        // finally run the task we want to check
        project.tasks.modifyRelease.modifyTrack()
    }

    @Test
    void whenModifyTrack_withNoFromTrack_defaultsToInternal() {
        def project = TestHelper.evaluatableProject()

        project.play {
        }

        project.evaluate()

        // Attach the mock
        setMockPublisher(project.tasks.modifyRelease)

        // finally run the task we want to check
        project.tasks.modifyRelease.modifyTrack()

        verify(getInternalTrackMock).execute()
    }

    @Test
    void whenModifyTrack_withFromTrackAndNoToTrack_usesFromTrack() {
        def project = TestHelper.evaluatableProject()

        project.play {
            releaseModifiers {
                fromTrack = "internal"
            }
        }

        project.evaluate()

        // Attach the mock
        setMockPublisher(project.tasks.modifyRelease)

        project.tasks.modifyRelease.modifyTrack()

        verify(editTracksMock).update(
                eq('com.example.publisher'),
                eq('424242'),
                eq('internal'),
                any(Track.class))
    }

    @Test
    void whenModifyTrack_withFromTrackAndToTrack() {
        def project = TestHelper.evaluatableProject()

        project.play {
            releaseModifiers {
                fromTrack = "internal"
                toTrack = "alpha"
            }
        }

        project.evaluate()

        // Attach the mock
        setMockPublisher(project.tasks.modifyRelease)

        project.tasks.modifyRelease.modifyTrack()

        verify(getInternalTrackMock).execute()

        verify(editTracksMock).update(
                eq('com.example.publisher'),
                eq('424242'),
                eq('alpha'),
                any(Track.class))
    }

    @Test(expected = IllegalArgumentException)
    void whenModifyTrack_withFromTrackAndBackwardsToTrack() {
        def project = TestHelper.evaluatableProject()

        project.play {
            releaseModifiers {
                fromTrack = "beta"
                toTrack = "alpha"
            }
        }

        project.

        project.evaluate()
    }

    @Test
    void whenModifyTrack_withReleaseStatus() {
        def project = TestHelper.evaluatableProject()

        project.play {
            releaseModifiers {
                fromTrack = "internal"
                toTrack = "alpha"
                releaseStatus = "draft"
            }
        }

        project.evaluate()

        // Attach the mock
        setMockPublisher(project.tasks.modifyRelease)

        project.tasks.modifyRelease.modifyTrack()

        verify(getInternalTrackMock).execute()

        verify(editTracksMock).update(
                eq('com.example.publisher'),
                eq('424242'),
                eq('alpha'),
                any(Track.class))
//                argThat { track ->
//                    track.releases.firstOrNull()
//                } as Track)


    }

    private void setMockPublisher(Task task) {
        def field = task.class.superclass.superclass.getDeclaredField("publisher\$delegate")
        field.setAccessible(true)
        field.set(task, LazyKt.lazy { publisherMock })
    }

}
