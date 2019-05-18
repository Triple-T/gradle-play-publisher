package com.github.triplet.gradle.play

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

import static com.github.triplet.gradle.play.TestHelper.FIXTURE_WORKING_DIR
import static com.github.triplet.gradle.play.TestHelper.execute
import static junit.framework.TestCase.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class GenerateResourcesTest {

    @Test
    void testResourcesAreCopiedIntoOutputFolder() {
        execute("", "clean", "generateReleasePlayResources")

        assertTrue(new File(FIXTURE_WORKING_DIR, 'build/generated/gpp').exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/release').exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/release/res/listings/en-US').exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/release/res/listings/fr-FR').exists())

        def content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/release/res/release-notes/en-US/default.txt').text
        assertEquals('main', content)
        content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/release/res/release-notes/fr-FR/default.txt').text
        assertEquals('main', content)

        content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/release/res/products/sku.json').text
        assertEquals(new File(FIXTURE_WORKING_DIR, 'src/main/play/products/sku.json').text, content)
    }

    @Test
    void invalidProductThrows() {
        // language=gradle
        def config = """
            flavorDimensions 'pricing'

            productFlavors {
                invalidProduct { dimension 'pricing' }
            }
        """
        def result = execute(config, true, "clean", "generateInvalidProductReleasePlayResources")

        assertEquals(TaskOutcome.FAILED, result.task(":generateInvalidProductReleasePlayResources").outcome)
    }

    @Test
    void testFlavorsOverrideMain() {
        // language=gradle
        def config = """
            flavorDimensions 'pricing'

            productFlavors {
                free {
                    dimension 'pricing'
                }
                paid {
                    dimension 'pricing'
                }
            }
        """
        execute(config, "clean", "generateFreeReleasePlayResources")

        assertTrue(new File(FIXTURE_WORKING_DIR, 'build/generated/gpp').exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease').exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/listings/de-DE').exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/listings/en-US').exists())
        assertTrue(new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/listings/fr-FR').exists())

        def content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/release-notes/de-DE/default.txt').text
        assertEquals('free german', content)
        content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/release-notes/fr-FR/default.txt').text
        assertEquals('main', content)
        content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/release-notes/en-US/default.txt').text
        assertEquals('free', content)

        execute(config, "generatePaidReleasePlayResources")

        content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/paidRelease/res/release-notes/de-DE/default.txt').text
        assertEquals('paid german', content)
        content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/paidRelease/res/release-notes/fr-FR/default.txt').text
        assertEquals('main', content)
        content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/paidRelease/res/release-notes/en-US/default.txt').text
        assertEquals('paid english', content)
    }

    @Test
    void testBuildTypeOverridesMain() {
        // language=gradle
        def config = """
            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        """
        execute(config, "clean", "generateDogfoodPlayResources")

        def content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/dogfood/res/release-notes/en-US/default.txt').text
        assertEquals('dogfood english', content)
    }

    @Test
    void testBuildTypeOverridesFlavor() {
        // language=gradle
        def config = """
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
        """
        execute(config, "clean", "generatePaidDogfoodPlayResources")

        def content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/paidDogfood/res/release-notes/en-US/default.txt').text
        assertEquals('dogfood english', content)
    }

    @Test
    void testVariantOverridesBuildType() {
        // language=gradle
        def config = """
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
        """
        execute(config, "clean", "generateFreeDogfoodPlayResources")

        def content = new File(FIXTURE_WORKING_DIR, 'build/generated/gpp/freeDogfood/res/release-notes/en-US/default.txt').text
        assertEquals('free dogfood english', content)
    }

    @Test
    void multidimensionalResourcesAreNotOverwritten() {
        def originalReleaseNotes = new File(FIXTURE_WORKING_DIR,
                'src/freeStagingDogfood/play/release-notes/en-US/default.txt').text
        def originalFullDescription = new File(FIXTURE_WORKING_DIR,
                'src/freeStagingDogfood/play/listings/en-US/full-description.txt').text

        // language=gradle
        def config = """
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
        """
        execute(config, "clean", "generateFreeStagingDogfoodPlayResources")

        def processedReleaseNotes = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingDogfood/res/release-notes/en-US/default.txt').text
        def processedFullDescription = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingDogfood/res/listings/en-US/full-description.txt').text

        assertEquals(originalReleaseNotes, processedReleaseNotes)
        assertEquals(originalFullDescription, processedFullDescription)
    }

    @Test
    void ignoresHiddenFile() {
        // language=gradle
        def config = """
            flavorDimensions 'pricing'

            productFlavors {
                hiddenFile { dimension 'pricing' }
            }
        """
        def result = execute(config, "clean", "generateHiddenFileReleasePlayResources")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateHiddenFileReleasePlayResources").outcome)
    }

    @Test
    void invalidLocaleThrows() {
        // language=gradle
        def config = """
            flavorDimensions 'pricing'

            productFlavors {
                invalidLocale { dimension 'pricing' }
            }
        """
        def result = execute(config, true, "clean", "generateInvalidLocaleReleasePlayResources")

        assertEquals(TaskOutcome.FAILED, result.task(":generateInvalidLocaleReleasePlayResources").outcome)
    }

    @Test
    void fileInWrongDirThrows() {
        // language=gradle
        def config = """
            flavorDimensions 'pricing'

            productFlavors {
                unknownFile { dimension 'pricing' }
            }
        """
        def result = execute(config, true, "clean", "generateUnknownFileReleasePlayResources")

        assertEquals(TaskOutcome.FAILED, result.task(":generateUnknownFileReleasePlayResources").outcome)
    }

    @Test
    void multidimensionalFlavorsMerge() {
        def originalReleaseNotes = new File(FIXTURE_WORKING_DIR,
                'src/freeStaging/play/release-notes/en-US/default.txt').text
        def originalFullDescription = new File(FIXTURE_WORKING_DIR,
                'src/freeStagingRelease/play/listings/en-US/full-description.txt').text

        // language=gradle
        def config = """
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """
        execute(config, "clean", "generateFreeStagingReleasePlayResources")

        def processedReleaseNotes = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/release-notes/en-US/default.txt').text
        def processedFullDescription = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/listings/en-US/full-description.txt').text

        assertEquals(originalReleaseNotes, processedReleaseNotes)
        assertEquals(originalFullDescription, processedFullDescription)
    }

    @Test
    void flavorMerge() {
        def originalFullDescription = new File(FIXTURE_WORKING_DIR,
                'src/freeStagingRelease/play/listings/en-US/full-description.txt').text
        def originalShortDescription = new File(FIXTURE_WORKING_DIR,
                'src/free/play/listings/en-US/short-description.txt').text

        // language=gradle
        def config = """
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """
        execute(config, "clean", "generateFreeStagingReleasePlayResources")

        def processedFullDescription = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/listings/en-US/full-description.txt').text
        def processedShortDescription = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/listings/en-US/short-description.txt').text

        assertEquals(originalFullDescription, processedFullDescription)
        assertEquals(originalShortDescription, processedShortDescription)
    }

    @Test
    void flavorDimensionOrderDeterminesConflictingFlavorWinner() {
        def originalReleaseNotes = new File(FIXTURE_WORKING_DIR,
                'src/prod/play/release-notes/en-US/default.txt').text

        // language=gradle
        def config = """
            flavorDimensions 'server', 'pricing'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """
        execute(config, "clean", "generateProdFreeReleasePlayResources")

        def processedReleaseNotes = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodFreeRelease/res/release-notes/en-US/default.txt').text

        assertEquals(originalReleaseNotes, processedReleaseNotes)
    }

    @Test
    void mainMerge() {
        def originalFullDescription = new File(FIXTURE_WORKING_DIR,
                'src/freeStagingRelease/play/listings/en-US/full-description.txt').text
        def originalTitle = new File(FIXTURE_WORKING_DIR,
                'src/main/play/listings/en-US/title.txt').text

        // language=gradle
        def config = """
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """
        execute(config, "clean", "generateFreeStagingReleasePlayResources")

        def processedFullDescription = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/listings/en-US/full-description.txt').text
        def processedTitle = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/listings/en-US/title.txt').text

        assertEquals(originalFullDescription, processedFullDescription)
        assertEquals(originalTitle, processedTitle)
    }

    @Test
    void languageMerge() {
        def originalTitle = new File(FIXTURE_WORKING_DIR,
                'src/main/play/listings/en-US/title.txt').text
        def originalFullDescription = new File(FIXTURE_WORKING_DIR,
                'src/main/play/listings/de-DE/full-description.txt').text
        def originalShortDescription = new File(FIXTURE_WORKING_DIR,
                'src/staging/play/listings/en-US/short-description.txt').text

        // language=gradle
        def config = """
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """
        execute(config, "clean", "generateProdStagingReleasePlayResources")

        def processedTitle = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/de-DE/title.txt').text
        def processedFullDescription = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/de-DE/full-description.txt').text
        def processedShortDescription = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/de-DE/short-description.txt').text

        assertEquals(originalTitle, processedTitle)
        assertEquals(originalFullDescription, processedFullDescription)
        assertEquals(originalShortDescription, processedShortDescription)
    }

    @Test
    void graphicsLanguageMergeOnlyAcrossCategories() {
        // language=gradle
        def config = """
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """
        execute(config, "clean", "generateProdStagingReleasePlayResources")

        def processedFrPhone = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/fr-FR/graphics/phone-screenshots/foo.jpg')
        def existingFrPhone = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/fr-FR/graphics/phone-screenshots/bar.jpg')
        def processedFrTablet = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/fr-FR/graphics/tablet-screenshots/baz.jpg')
        def processedDePhone = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/de-DE/graphics/phone-screenshots/foo.jpg')
        def processedDeTablet = new File(FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/de-DE/graphics/tablet-screenshots/baz.jpg')

        assertFalse(processedFrPhone.exists())
        assertTrue(existingFrPhone.exists())
        assertTrue(processedFrTablet.exists())
        assertTrue(processedDePhone.exists())
        assertTrue(processedDeTablet.exists())
    }
}
