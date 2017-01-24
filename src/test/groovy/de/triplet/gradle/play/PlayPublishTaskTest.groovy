package de.triplet.gradle.play

import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.AppEdit
import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.Project
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Matchers.argThat
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.MockitoAnnotations.initMocks

public class PlayPublishTaskTest {

    @Mock
    AndroidPublisher publisherMock

    @Mock
    AndroidPublisher.Edits editsMock

    @Mock
    AndroidPublisher.Edits.Insert insertMock

    @Mock
    AndroidPublisher.Edits.Tracks editTracksMock;

    @Mock
    AndroidPublisher.Edits.Commit editCommitMock;

    @Mock
    AndroidPublisher.Edits.Tracks.Update tracksUpdateMock;

    @Mock
    AndroidPublisher.Edits.Tracks.Get getAlphaTrackMock

    @Mock
    AndroidPublisher.Edits.Tracks.Get getBetaTrackMock

    @Mock
    AndroidPublisher.Edits.Apks apksMock

    @Mock
    AndroidPublisher.Edits.Apks.Upload uploadMock

    // These are final and not mock able
    AppEdit appEdit = new AppEdit()
    Apk apk = new Apk();
    Track alphaTrack = new Track();
    Track betaTrack = new Track();

    @Before
    public void setup() {
        initMocks(this)

        appEdit.setId("424242")
        apk.setVersionCode(42)

        doReturn(editsMock).when(publisherMock).edits()
        doReturn(insertMock).when(editsMock).insert(anyString(), any(AppEdit.class))
        doReturn(appEdit).when(insertMock).execute()

        doReturn(apksMock).when(editsMock).apks()
        doReturn(uploadMock).when(apksMock).upload(anyString(), anyString(), any(FileContent.class))
        doReturn(apk).when(uploadMock).execute()

        doReturn(editTracksMock).when(editsMock).tracks();
        doReturn(getAlphaTrackMock).when(editTracksMock).get(anyString(), anyString(), eq('alpha'))
        doReturn(alphaTrack).when(getAlphaTrackMock).execute()
        doReturn(getBetaTrackMock).when(editTracksMock).get(anyString(), anyString(), eq('beta'))
        doReturn(betaTrack).when(getBetaTrackMock).execute()

        doReturn(tracksUpdateMock).when(editTracksMock).update(anyString(), anyString(), anyString(), any(Track.class));
        doReturn(editCommitMock).when(editsMock).commit(anyString(), anyString());
    }

    @Test
    public void testApplicationId() {
        Project project = TestHelper.evaluatableProject()
        project.evaluate()

        // Attach the mock
        project.tasks.publishApkRelease.service = publisherMock

        // finally run the task we want to check
        project.tasks.publishApkRelease.publish()

        // verify that we init the connection with the correct application id
        verify(editsMock).insert("com.example.publisher", null)
    }

    @Test
    public void testApplicationIdWithFlavorsAndSuffix() {
        Project project = TestHelper.evaluatableProject()

        project.android {
            productFlavors {
                paid {
                    applicationId 'com.example.publisher.paid'
                }
                free
            }

            buildTypes {
                release {
                    applicationIdSuffix '.release'
                }
            }
        }

        project.evaluate()

        // Attach the mock
        project.tasks.publishApkPaidRelease.service = publisherMock

        // finally run the task we want to check
        project.tasks.publishApkPaidRelease.publish()

        // verify that we init the connection with the correct application id
        verify(editsMock).insert("com.example.publisher.paid.release", null)
    }

    @Test
    public void whenPublishingToBeta_publishApkRelease_removesBlockingVersionsFromAlpha() {
        Project project = TestHelper.evaluatableProject()
        project.play {
            track 'beta'
            untrackOld true
        }
        project.evaluate()

        alphaTrack.setVersionCodes(new ArrayList<Integer>([41, 40]))

        project.tasks.publishApkRelease.service = publisherMock
        project.tasks.publishApkRelease.publishApks()

        verify(editTracksMock).update(
                eq('com.example.publisher'),
                eq('424242'),
                eq('alpha'),
                emptyTrack())
    }

    @Test
    public void whenPublishingToBeta_publishApkRelease_doesNotRemoveNonBlockingVersionsFromAlpha() {
        Project project = TestHelper.evaluatableProject()
        project.play {
            track 'beta'
            untrackOld true
        }
        project.evaluate()

        alphaTrack.setVersionCodes(new ArrayList<Integer>([43]))

        project.tasks.publishApkRelease.service = publisherMock
        project.tasks.publishApkRelease.publishApks()

        verify(editTracksMock).update(
                eq('com.example.publisher'),
                eq('424242'),
                eq('alpha'),
                trackThatContains(43))
    }

    @Test
    public void whenPublishingToProduction_publishApkRelease_removesBlockingVersionFromAlphaAndBeta() {
        Project project = TestHelper.evaluatableProject()
        project.play {
            track 'production'
            untrackOld true
        }
        project.evaluate()

        alphaTrack.setVersionCodes(new ArrayList<Integer>([40, 41]))
        betaTrack.setVersionCodes(new ArrayList<Integer>([39]))

        project.tasks.publishApkRelease.service = publisherMock
        project.tasks.publishApkRelease.publishApks()

        verify(editTracksMock).update(
                eq('com.example.publisher'),
                eq('424242'),
                eq('alpha'),
                emptyTrack())

        verify(editTracksMock).update(
                eq('com.example.publisher'),
                eq('424242'),
                eq('beta'),
                emptyTrack())
    }

    @Test
    public void whenPublishingToProduction_publishApkRelease_doesNotRemoveNonBlockingVersionFromAlphaOrBeta() {
        Project project = TestHelper.evaluatableProject()
        project.play {
            track 'production'
            untrackOld true
        }
        project.evaluate()

        alphaTrack.setVersionCodes(new ArrayList<Integer>([44]))
        betaTrack.setVersionCodes(new ArrayList<Integer>([43]))

        project.tasks.publishApkRelease.service = publisherMock
        project.tasks.publishApkRelease.publishApks()

        verify(editTracksMock).update(
                eq('com.example.publisher'),
                eq('424242'),
                eq('alpha'),
                trackThatContains(44))

        verify(editTracksMock).update(
                eq('com.example.publisher'),
                eq('424242'),
                eq('beta'),
                trackThatContains(43))
    }

    @Test
    public void whenFlagNotSet_publishApkRelease_doesNotTouchOtherTracks() {
        Project project = TestHelper.evaluatableProject()
        project.play {
            track 'production'
            untrackOld false
        }
        project.evaluate()

        verify(editTracksMock, times(0)).update(anyString(), anyString(), eq('alpha'), any(Track.class))
        verify(editTracksMock, times(0)).update(anyString(), anyString(), eq('beta'), any(Track.class))
    }

    @Test
    public void testMatcher() {
        Project project = TestHelper.evaluatableProject()
        project.evaluate()

        Pattern pattern = project.tasks.publishApkRelease.matcher

        Matcher m = pattern.matcher("de-DE")
        assertTrue(m.find())

        m = pattern.matcher("de")
        assertTrue(m.find())

        m = pattern.matcher("es-419")
        assertTrue(m.find())

        m = pattern.matcher("fil")
        assertTrue(m.find())

        m = pattern.matcher("de_DE")
        assertFalse(m.find())

        m = pattern.matcher("fil-PH")
        assertFalse(m.find())
    }

    @Test
    public void testApplicationIdChange() {
        Project project = TestHelper.evaluatableProject()

        project.android {
            productFlavors {
                paid {
                    applicationId 'com.example.publisher'
                }
                free
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
        project.tasks.publishApkPaidRelease.service = publisherMock

        // finally run the task we want to check
        project.tasks.publishApkPaidRelease.publish()

        // verify that we init the connection with the correct application id
        verify(editsMock).insert("com.example.publisher.paid.release", null)
    }

    static Track emptyTrack() {
        return argThat(new TypeSafeMatcher<Track>() {
            @Override
            protected boolean matchesSafely(Track track) {
                return track.getVersionCodes().size() == 0
            }

            @Override
            void describeTo(Description description) {

            }
        })
    }

    static Track trackThatContains(final int code) {
        return argThat(new TypeSafeMatcher<Track>() {
            @Override
            protected boolean matchesSafely(Track track) {
                return track.getVersionCodes().contains(code)
            }

            @Override
            void describeTo(Description description) {

            }
        })
    }
}
