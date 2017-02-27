package de.triplet.gradle.play

import org.apache.commons.io.FileUtils
import org.junit.Test

import static junit.framework.TestCase.assertEquals
import static org.junit.Assert.assertTrue

class GenerateResourcesTest {

    @Test
    public void testResourcesAreCopiedIntoOutputFolder() {
        def project = TestHelper.evaluatableProject()

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateReleasePlayResources.execute()

        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/en-US').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/fr-FR').exists())

        def content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/en-US/whatsnew'))
        assertEquals('main english', content)
        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/fr-FR/whatsnew'))
        assertEquals('main french', content)
    }

    @Test
    public void testFlavorsOverrideMain() {
        def project = TestHelper.evaluatableProject()

        project.android.productFlavors {
            free
            paid
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFreeReleasePlayResources.execute()

        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/de-DE').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/en-US').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/fr-FR').exists())

        def content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/de-DE/whatsnew'))
        assertEquals('free german', content)
        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/fr-FR/whatsnew'))
        assertEquals('main french', content)
        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/en-US/whatsnew'))
        assertEquals('main english', content)

        project.tasks.generatePaidReleasePlayResources.execute()

        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/de-DE/whatsnew'))
        assertEquals('paid german', content)
        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/fr-FR/whatsnew'))
        assertEquals('main french', content)
        content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/en-US/whatsnew'))
        assertEquals('paid english', content)
    }

    @Test
    public void testBuildTypeOverridesMain() {
        def project = TestHelper.evaluatableProject()

        project.android {
            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateDogfoodPlayResources.execute()

        def content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/dogfood/en-US/whatsnew'))
        assertEquals('dogfood english', content)
    }

    @Test
    public void testBuildTypeOverridesFlavor() {
        def project = TestHelper.evaluatableProject()

        project.android {
            productFlavors {
                free
                paid
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generatePaidDogfoodPlayResources.execute()

        def content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidDogfood/en-US/whatsnew'))
        assertEquals('dogfood english', content)
    }

    @Test
    public void testVariantOverridesBuildType() {
        def project = TestHelper.evaluatableProject()

        project.android {
            productFlavors {
                free
                paid
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFreeDogfoodPlayResources.execute()

        def content = FileUtils.readFileToString(
                new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeDogfood/en-US/whatsnew'))
        assertEquals('free dogfood english', content)
    }

}
