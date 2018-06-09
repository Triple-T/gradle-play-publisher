package com.github.triplet.gradle.play

import org.junit.Test

import static junit.framework.TestCase.assertEquals
import static org.junit.Assert.assertTrue

class GenerateResourcesTest {

    @Test
    void testResourcesAreCopiedIntoOutputFolder() {
        def project = TestHelper.evaluatableProject()

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateReleasePlayResources.execute()

        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/en-US').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/fr-FR').exists())

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/en-US/whatsnew').text
        assertEquals('main english', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/fr-FR/whatsnew').text
        assertEquals('main french', content)
    }

    @Test
    void testFlavorsOverrideMain() {
        def project = TestHelper.evaluatableProject()

        project.android {
            flavorDimensions 'pricing'

            productFlavors {
                free {
                    dimension 'pricing'
                }
                paid {
                    dimension 'pricing'
                }
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFreeReleasePlayResources.execute()

        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/de-DE').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/en-US').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/fr-FR').exists())

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/de-DE/whatsnew').text
        assertEquals('free german', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/fr-FR/whatsnew').text
        assertEquals('main french', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/en-US/whatsnew').text
        assertEquals('main english', content)

        project.tasks.generatePaidReleasePlayResources.execute()

        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/res/de-DE/whatsnew').text
        assertEquals('paid german', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/res/fr-FR/whatsnew').text
        assertEquals('main french', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/res/en-US/whatsnew').text
        assertEquals('paid english', content)
    }

    @Test
    void testBuildTypeOverridesMain() {
        def project = TestHelper.evaluatableProject()

        project.android {
            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateDogfoodPlayResources.execute()

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/dogfood/res/en-US/whatsnew').text
        assertEquals('dogfood english', content)
    }

    @Test
    void testBuildTypeOverridesFlavor() {
        def project = TestHelper.evaluatableProject()

        project.android {
            flavorDimensions 'pricing'

            productFlavors {
                free {
                    dimension 'pricing'
                }
                paid {
                    dimension 'pricing'
                }
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generatePaidDogfoodPlayResources.execute()

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidDogfood/res/en-US/whatsnew').text
        assertEquals('dogfood english', content)
    }

    @Test
    void testVariantOverridesBuildType() {
        def project = TestHelper.evaluatableProject()

        project.android {
            flavorDimensions 'pricing'

            productFlavors {
                free {
                    dimension 'pricing'
                }
                paid {
                    dimension 'pricing'
                }
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFreeDogfoodPlayResources.execute()

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeDogfood/res/en-US/whatsnew').text
        assertEquals('free dogfood english', content)
    }

}
