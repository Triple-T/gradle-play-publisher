package de.triplet.gradle.play

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static junit.framework.TestCase.assertEquals
import static junit.framework.TestCase.assertFalse
import static org.junit.Assert.*

class GenerateResourcesTest {

    @Test
    public void testGenerateTask() {
        Project project = TestHelper.evaluatableProject()

        project.android.productFlavors {
            free
            paid
        }

        project.evaluate()

        assertNotNull(project.tasks.generateFreeReleasePlayResources)
        assertNotNull(project.tasks.generatePaidReleasePlayResources)

        project.tasks.clean.execute()

        assertFalse(new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play").exists())

        project.tasks.generateFreeReleasePlayResources.execute()

        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play").exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease").exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/de-DE").exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/en-US").exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/fr-FR").exists())

        String content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/de-DE/whatsnew"))
        assertEquals("free german", content)
        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/fr-FR/whatsnew"))
        assertEquals("main french", content)
        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play/freeRelease/en-US/whatsnew"))
        assertEquals("main english", content)

        project.tasks.generatePaidReleasePlayResources.execute()

        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play/paidRelease/de-DE/whatsnew"))
        assertEquals("paid german", content)
        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play/paidRelease/fr-FR/whatsnew"))
        assertEquals("main french", content)
        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, "build/outputs/play/paidRelease/en-US/whatsnew"))
        assertEquals("paid english", content)
    }

}
