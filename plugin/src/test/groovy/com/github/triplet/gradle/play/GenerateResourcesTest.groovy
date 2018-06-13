package com.github.triplet.gradle.play

import org.gradle.api.tasks.TaskExecutionException
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
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/listings/en-US').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/listings/fr-FR').exists())

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/release-notes/en-US/default.txt').text
        assertEquals('main', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/release-notes/fr-FR/default.txt').text
        assertEquals('main', content)
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
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/listings/de-DE').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/listings/en-US').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/listings/fr-FR').exists())

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/release-notes/de-DE/default.txt').text
        assertEquals('free german', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/release-notes/fr-FR/default.txt').text
        assertEquals('main', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/release-notes/en-US/default.txt').text
        assertEquals('main', content)

        project.tasks.generatePaidReleasePlayResources.execute()

        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/res/release-notes/de-DE/default.txt').text
        assertEquals('paid german', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/res/release-notes/fr-FR/default.txt').text
        assertEquals('main', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/res/release-notes/en-US/default.txt').text
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

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/dogfood/res/release-notes/en-US/default.txt').text
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

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidDogfood/res/release-notes/en-US/default.txt').text
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

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeDogfood/res/release-notes/en-US/default.txt').text
        assertEquals('free dogfood english', content)
    }

    @Test
    void multidimensionalResourcesAreNotOverwritten() {
        def project = TestHelper.evaluatableProject()
        def originalReleaseNotes = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStagingDogfood/play/release-notes/en-US/default.txt').text
        def originalFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStagingDogfood/play/listings/en-US/fulldescription').text

        project.android {
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFreeStagingDogfoodPlayResources.execute()

        def processedReleaseNotes = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/freeStagingDogfood/res/release-notes/en-US/default.txt').text
        def processedFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/freeStagingDogfood/res/listings/en-US/fulldescription').text

        assertEquals(originalReleaseNotes, processedReleaseNotes)
        assertEquals(originalFullDescription, processedFullDescription)
    }

    @Test(expected = TaskExecutionException)
    void conflictingFlavorsThrows() {
        def project = TestHelper.evaluatableProject()

        project.android {
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFreeProdDogfoodPlayResources.execute()
    }

    @Test(expected = TaskExecutionException)
    void invalidLocaleThrows() {
        def project = TestHelper.evaluatableProject()

        project.android {
            flavorDimensions 'pricing'

            productFlavors {
                invalidLocale { dimension 'pricing' }
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateInvalidLocaleReleasePlayResources.execute()
    }

    @Test(expected = TaskExecutionException)
    void fileInWrongDirThrows() {
        def project = TestHelper.evaluatableProject()

        project.android {
            flavorDimensions 'pricing'

            productFlavors {
                unknownFile { dimension 'pricing' }
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateUnknownFileReleasePlayResources.execute()
    }

    @Test
    void multidimensionalFlavorsMerge() {
        def project = TestHelper.evaluatableProject()
        def originalReleaseNotes = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStaging/play/release-notes/en-US/default.txt').text
        def originalFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStagingRelease/play/listings/en-US/fulldescription').text

        project.android {
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFreeStagingReleasePlayResources.execute()

        def processedReleaseNotes = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/freeStagingRelease/res/release-notes/en-US/default.txt').text
        def processedFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/freeStagingRelease/res/listings/en-US/fulldescription').text

        assertEquals(originalReleaseNotes, processedReleaseNotes)
        assertEquals(originalFullDescription, processedFullDescription)
    }

    @Test
    void flavorMerge() {
        def project = TestHelper.evaluatableProject()
        def originalFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStagingRelease/play/listings/en-US/fulldescription').text
        def originalShortDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/free/play/listings/en-US/shortdescription').text

        project.android {
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFreeStagingReleasePlayResources.execute()

        def processedFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/freeStagingRelease/res/listings/en-US/fulldescription').text
        def processedShortDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/freeStagingRelease/res/listings/en-US/shortdescription').text

        assertEquals(originalFullDescription, processedFullDescription)
        assertEquals(originalShortDescription, processedShortDescription)
    }

    @Test
    void mainMerge() {
        def project = TestHelper.evaluatableProject()
        def originalFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStagingRelease/play/listings/en-US/fulldescription').text
        def originalTitle = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/main/play/listings/en-US/title').text

        project.android {
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFreeStagingReleasePlayResources.execute()

        def processedFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/freeStagingRelease/res/listings/en-US/fulldescription').text
        def processedTitle = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/freeStagingRelease/res/listings/en-US/title').text

        assertEquals(originalFullDescription, processedFullDescription)
        assertEquals(originalTitle, processedTitle)
    }
}
