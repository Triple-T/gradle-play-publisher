package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.TrackType
import com.github.triplet.gradle.play.tasks.internal.PlayPublishTaskBase
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.AppEdit
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TracksListResponse
import kotlin.LazyKt
import org.gradle.api.Task
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.ArgumentMatchers.nullable
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.verify
import static org.mockito.MockitoAnnotations.initMocks

class PlayPublishTaskTest {

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
    AndroidPublisher.Edits.Apks apksMock

    @Mock
    AndroidPublisher.Edits.Apks.Upload uploadMock

    @Mock
    IncrementalTaskInputs inputsMock

    // These are final and not mock able
    final AppEdit appEdit = new AppEdit()
    final Apk apk = new Apk()
    final Track internalTrack = new Track()
    final Track alphaTrack = new Track()
    final Track betaTrack = new Track()
    final List<Track> tracksList = [
            new Track().setTrack(TrackType.INTERNAL.publishedName),
            new Track().setTrack(TrackType.ALPHA.publishedName),
            new Track().setTrack(TrackType.BETA.publishedName),
            new Track().setTrack(TrackType.PRODUCTION.publishedName)
    ]
    final TracksListResponse tracksListResponse = new TracksListResponse().setTracks(tracksList)

    @Before
    void setup() {
        initMocks(this)

        appEdit.setId('424242')
        apk.setVersionCode(42)

        doReturn(editsMock).when(publisherMock).edits()
        doReturn(insertMock).when(editsMock).insert(anyString(), nullable(AppEdit.class))
        doReturn(appEdit).when(insertMock).execute()

        doReturn(editTracksListMock).when(editTracksMock).list(anyString(), anyString())
        doReturn(tracksListResponse).when(editTracksListMock).execute()

        doReturn(apksMock).when(editsMock).apks()
        doReturn(uploadMock).when(apksMock).upload(anyString(), anyString(), nullable(FileContent.class))
        doReturn(apk).when(uploadMock).execute()

        doReturn(editTracksMock).when(editsMock).tracks()
        doReturn(getInternalTrackMock).when(editTracksMock).get(anyString(), anyString(), eq('internal'))
        doReturn(internalTrack).when(getInternalTrackMock).execute()
        doReturn(getAlphaTrackMock).when(editTracksMock).get(anyString(), anyString(), eq('alpha'))
        doReturn(alphaTrack).when(getAlphaTrackMock).execute()
        doReturn(getBetaTrackMock).when(editTracksMock).get(anyString(), anyString(), eq('beta'))
        doReturn(betaTrack).when(getBetaTrackMock).execute()

        doReturn(tracksUpdateMock).when(editTracksMock).update(anyString(), anyString(), anyString(), nullable(Track.class))
        doReturn(editCommitMock).when(editsMock).commit(anyString(), anyString())
    }

    @Test
    void testApplicationIdChange() {
        def project = TestHelper.evaluatableProject()

        project.android {
            flavorDimensions 'pricing'

            productFlavors {
                paid {
                    dimension 'pricing'
                    applicationId 'com.example.publisher'
                }
                free {
                    dimension 'pricing'
                }
            }

            buildTypes {
                release {
                    applicationIdSuffix '.release'
                }
            }

            applicationVariants.all { variant ->
                def flavorName = variant.variantData.variantConfiguration.flavorName
                if (flavorName == 'paid') {
                    variant.mergedFlavor.applicationId += '.paid'
                }
            }
        }

        project.evaluate()

        // Attach the mock
        setMockPublisher(project.tasks.publishPaidReleaseApk)

        // finally run the task we want to check
        project.tasks.publishPaidReleaseApk.publishApks(inputsMock)

        // verify that we init the connection with the correct application id
        verify(editsMock).insert('com.example.publisher.paid.release', null)
    }

    private void setMockPublisher(Task task) {
        def field = findBaseTask(task.class, PlayPublishTaskBase.class)
                .getDeclaredField("publisher\$delegate")
        field.setAccessible(true)
        field.set(task, LazyKt.lazy { publisherMock })
    }

    private Class<? super Task> findBaseTask(Class<? super Task> start, Class<? super Task> end) {
        if (start == end) {
            return end as Class<? super Task>
        } else {
            return findBaseTask(start.superclass, end)
        }
    }
}
