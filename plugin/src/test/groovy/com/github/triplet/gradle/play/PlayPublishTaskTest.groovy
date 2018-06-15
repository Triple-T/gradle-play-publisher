package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.internal.TrackType
import com.github.triplet.gradle.play.tasks.PublishApk
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.AppEdit
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import com.google.api.services.androidpublisher.model.TracksListResponse
import kotlin.LazyKt
import org.gradle.api.Task
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.argThat
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
    void whenPublishing_withOverrideFolder_UploadsOnlySpecifiedFiles() {
        def project = TestHelper.evaluatableProject()
        project.play {
            buildInputFolder new File(TestHelper.FIXTURE_WORKING_DIR, "override_output")
        }
        project.evaluate()

        setMockPublisher(project.tasks.publishApkRelease)
        publishApk(project.tasks.publishApkRelease, "example_1.apk")

        verify(apksMock).upload(
                eq('com.example.publisher'),
                eq('424242'),
                contentWithName("example_1.apk"))
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
        setMockPublisher(project.tasks.publishApkPaidRelease)

        // finally run the task we want to check
        project.tasks.publishApkPaidRelease.publishApks(inputsMock)

        // verify that we init the connection with the correct application id
        verify(editsMock).insert('com.example.publisher.paid.release', null)
    }

    private void setMockPublisher(Task task) {
        def field = findBaseTask(task.class, PlayPublishTaskBase.class)
                .getDeclaredField("publisher\$delegate")
        field.setAccessible(true)
        field.set(task, LazyKt.lazy { publisherMock })
    }

    private void publishApk(Task task) {
        publishApk(task, "foo")
    }

    private void publishApk(Task task, String fileName) {
        def progressLogger = findBaseTask(task.class, PlayPublishTaskBase.class)
                .getDeclaredField("progressLogger")
        progressLogger.setAccessible(true)
        progressLogger.get(task).start("Desc", null)

        def publishApk = findBaseTask(task.class, PublishApk.class).getDeclaredMethod(
                "publishApk", AndroidPublisher.Edits.class, String.class, FileContent.class)
        publishApk.setAccessible(true)
        publishApk.invoke(task, editsMock, "424242", new FileContent(null, new File(fileName)))
    }

    private Class<? super Task> findBaseTask(Class<? super Task> start, Class<? super Task> end) {
        if (start == end) {
            return end as Class<? super Task>
        } else {
            return findBaseTask(start.superclass, end)
        }
    }

    static Track emptyTrack() {
        return argThat(new ArgumentMatcher<Track>() {
            @Override
            boolean matches(Track track) {
                return track.getReleases().sum {
                    (it as TrackRelease).getVersionCodes().size()
                } == 0
            }
        })
    }

    static Track trackThatContains(final Long code) {
        return argThat(new ArgumentMatcher<Track>() {
            @Override
            boolean matches(Track track) {
                return track.getReleases().find { it.getVersionCodes().contains(code) } != null
            }
        })
    }

    static FileContent contentWithName(String name) {
        return argThat(new ArgumentMatcher<FileContent>() {
            @Override
            boolean matches(FileContent content) {
                return content.file.name == name
            }
        })
    }
}
