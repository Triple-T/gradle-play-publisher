package de.triplet.gradle.play
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.AppEdit
import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.Project
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.verifyZeroInteractions
import static org.mockito.MockitoAnnotations.initMocks

class PlayUntrackTaskTest {

  @Mock
  AndroidPublisher publisherMock

  @Mock
  AndroidPublisher.Edits editsMock

  @Mock
  AndroidPublisher.Edits.Insert insertMock

  @Mock
  AndroidPublisher.Edits.Tracks editTracks;

  @Mock
  AndroidPublisher.Edits.Commit editCommit;

  @Mock
  AndroidPublisher.Edits.Tracks.Update tracksUpdate;

  @Mock
  AndroidPublisher.Edits.Tracks.Get getTrack

  // These are final and not mock able
  AppEdit appEdit = new AppEdit()
  Track track = new Track();

  ArrayList<Integer> versions = [3,301]

  @Before
  public void setup() {
    initMocks(this)
    track.setVersionCodes(versions)

    doReturn(editsMock).when(publisherMock).edits()
    doReturn(insertMock).when(editsMock).insert(anyString(), any(AppEdit.class))
    doReturn(appEdit).when(insertMock).execute()

    doReturn(editTracks).when(editsMock).tracks();
    doReturn(getTrack).when(editTracks).get(anyString(), anyString(), anyString())
    doReturn(track).when(getTrack).execute()

    doReturn(tracksUpdate).when(editTracks).update(anyString(), anyString(), anyString(), any(Track.class));
    doReturn(track).when(tracksUpdate).execute()
    doReturn(editCommit).when(editsMock).commit(anyString(), anyString());
  }

  @Test
  public void testUntrackNeeded() {
    Project project = TestHelper.evaluatableProject()
    project.play {
      untrackFormat "a"
    }
    project.evaluate()
    assertFalse(project.tasks.untrackApkRelease.untrackNotNeeded())
  }

  @Test
  public void testUntrackDefault_notNeeded() {
    Project project = TestHelper.evaluatableProject()
    project.evaluate()
    assertTrue(project.tasks.untrackApkRelease.untrackNotNeeded())
  }

  @Test
  public void testUntrackNotNeeded() {
    Project project = TestHelper.evaluatableProject()
    project.evaluate()
    assertTrue(project.tasks.untrackApkRelease.untrackNotNeeded())
  }

  @Test
  public void testGetVersionsToKeep_matchAll() {
    Project project = TestHelper.evaluatableProject()
    project.play {
      untrackFormat "3.*"
    }
    project.evaluate()
    assertTrue(project.tasks.untrackApkRelease.getVersionsToKeep(versions).isEmpty())
  }

  @Test
  public void testGetVersionsToKeep_matchOne() {
    Project project = TestHelper.evaluatableProject()
    project.play {
      untrackFormat "^3[0-9]{2,4}\$"
    }
    project.evaluate()
    List<Integer> keptVersions = project.tasks.untrackApkRelease.getVersionsToKeep(versions)
    assertTrue(keptVersions.contains(versions[0]))
    assertFalse(keptVersions.contains(versions[1]))
  }

  @Test
  public void testGetVersionsToKeep_removeAll() {
    Project project = TestHelper.evaluatableProject()
    project.play {
      untrackFormat "*"
    }
    project.evaluate()
    assertTrue(project.tasks.untrackApkRelease.getVersionsToKeep(versions).isEmpty())
  }

  @Test
  public void testUntrack_notNeeded() {
    Project project = TestHelper.evaluatableProject()
    project.evaluate()
    project.tasks.untrackApkRelease.service = publisherMock;

    project.tasks.untrackApkRelease.untrack()
    verifyZeroInteractions(publisherMock.edits())
  }

  @Test
  public void testUntrack() {
    Project project = TestHelper.evaluatableProject()
    project.play {
      untrackFormat "^3[0-9]{2,4}\$"
    }
    project.evaluate()
    project.tasks.untrackApkRelease.service = publisherMock;

    project.tasks.untrackApkRelease.untrack()
    assertEquals(1, track.getVersionCodes().size())
    assertEquals(versions[0], track.getVersionCodes().get(0))
    verify(editTracks).update(anyString(), anyString(), anyString(), any(Track.class))
    verify(editsMock).commit(anyString(), anyString())
  }

  @Test
  public void testUntrack_channelMatch() {
    Project project = TestHelper.evaluatableProject()
    project.play {
      untrack "beta"
      untrackFormat "^3[0-9]{2,4}\$"
    }
    project.evaluate()
    project.tasks.untrackApkRelease.service = publisherMock;

    project.tasks.untrackApkRelease.untrack()
    assertEquals("beta", project.tasks.untrackApkRelease.channel)
  }
}
