package play

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static junit.framework.TestCase.assertEquals
import static junit.framework.TestCase.assertFalse
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class PlayPublisherPluginTest {

    public static final File FIXTURE_WORKING_DIR = new File("src/test/fixtures/android_app")

    @Test(expected = IllegalStateException.class)
    public void testThrowsOnLibraryProjects() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'android-library'
        project.apply plugin: 'play'
    }

    @Test
    public void testCreatesDefaultTask() {
        Project project = evaluatableProject()
        project.evaluate()

        assertNotNull(project.tasks.publishRelease)
        assertEquals(project.tasks.publishApkRelease.apkFile, project.tasks.zipalignRelease.outputFile)
    }

    @Test
    public void testCreatesFlavorTasks() {
        Project project = evaluatableProject()

        project.android.productFlavors {
            free
            paid
        }

        project.evaluate()

        assertNotNull(project.tasks.publishPaidRelease)
        assertNotNull(project.tasks.publishFreeRelease)

        assertEquals(project.tasks.zipalignPaidRelease.outputFile, project.tasks.publishApkPaidRelease.apkFile)
        assertEquals(project.tasks.zipalignFreeRelease.outputFile, project.tasks.publishApkFreeRelease.apkFile)
    }

    @Test
    public void testDefaultTrack() {
        Project project = evaluatableProject()
        project.evaluate()

        assertEquals('alpha', project.extensions.findByName("play").track)
    }

    @Test
    public void testTrack() {
        Project project = evaluatableProject()

        project.play {
            track 'production'
        }

        project.evaluate()

        assertEquals('production', project.extensions.findByName("play").track)
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTrack() {
        Project project = evaluatableProject()

        project.play {
            track 'gamma'
        }
    }

    @Test
    public void testGenerateTask() {
        Project project = evaluatableProject()

        project.android.productFlavors {
            free
            paid
        }

        project.evaluate()

        assertNotNull(project.tasks.generateFreeReleasePlayResources)
        assertNotNull(project.tasks.generatePaidReleasePlayResources)

        project.tasks.clean.execute()

        assertFalse(new File(FIXTURE_WORKING_DIR, "build/outputs/play").exists())

        project.tasks.generateFreeReleasePlayResources.execute()

        assertTrue(new File(FIXTURE_WORKING_DIR, "build/outputs/play").exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease").exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/de-DE").exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/en-US").exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/fr-FR").exists())

        String content = FileUtils.readFileToString(
                new File(FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/de-DE/whatsnew"))
        assertEquals("free german", content)
        content = FileUtils.readFileToString(
                new File(FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/fr-FR/whatsnew"))
        assertEquals("main french", content)
        content = FileUtils.readFileToString(
                new File(FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/en-US/whatsnew"))
        assertEquals("main english", content)

        project.tasks.generatePaidReleasePlayResources.execute()

        content = FileUtils.readFileToString(
                new File(FIXTURE_WORKING_DIR, "build/outputs/play/paidRelease/de-DE/whatsnew"))
        assertEquals("paid german", content)
        content = FileUtils.readFileToString(
                new File(FIXTURE_WORKING_DIR, "build/outputs/play/paidRelease/fr-FR/whatsnew"))
        assertEquals("main french", content)
        content = FileUtils.readFileToString(
                new File(FIXTURE_WORKING_DIR, "build/outputs/play/paidRelease/en-US/whatsnew"))
        assertEquals("paid english", content)
    }

    @Test
    public void testPublishListingTask() {
        Project project = evaluatableProject()

        project.android.productFlavors {
            free
            paid
        }

        project.evaluate()

        assertNotNull(project.tasks.publishListingFreeRelease)
        assertNotNull(project.tasks.publishListingPaidRelease)
    }

    def evaluatableProject() {
        Project project = ProjectBuilder.builder().withProjectDir(FIXTURE_WORKING_DIR).build();
        project.apply plugin: 'android'
        project.apply plugin: 'play'
        project.android {
            compileSdkVersion 20
            buildToolsVersion '20.0.0'

            buildTypes {
                release {
                    signingConfig signingConfigs.debug
                }
            }
        }

        return project
    }
}
