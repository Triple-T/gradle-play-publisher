package com.github.triplet.gradle.play

import org.junit.Test

import static com.github.triplet.gradle.play.TestHelper.cleanup
import static com.github.triplet.gradle.play.TestHelper.executeConnection
import static com.github.triplet.gradle.play.TestHelper.generateConnection
import static junit.framework.TestCase.assertEquals
import static org.junit.Assert.assertTrue

class GenerateResourcesTest {

    @Test
    void testResourcesAreCopiedIntoOutputFolder() {
        // language=gradle
        def connection = generateConnection("")

        executeConnection(connection, "clean")
        executeConnection(connection, "generateReleasePlayResources")

        cleanup()

        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/release').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/release/res/listings/en-US').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/release/res/listings/fr-FR').exists())

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/release/res/release-notes/en-US/default.txt').text
        assertEquals('main', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/release/res/release-notes/fr-FR/default.txt').text
        assertEquals('main', content)

        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/release/res/products/sku.json').text
        assertEquals(new File(TestHelper.FIXTURE_WORKING_DIR, 'src/main/play/products/sku.json').text, content)
    }

    @Test(expected = IllegalStateException)
    void invalidProductThrows() {
        // language=gradle
        def connection = generateConnection("""
            flavorDimensions 'pricing'

            productFlavors {
                invalidProduct { dimension 'pricing' }
            }
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateInvalidProductReleasePlayResources")

        cleanup()
    }

    @Test
    void testFlavorsOverrideMain() {
        // language=gradle
        def connection = generateConnection("""
            flavorDimensions 'pricing'

            productFlavors {
                free {
                    dimension 'pricing'
                }
                paid {
                    dimension 'pricing'
                }
            }
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateFreeReleasePlayResources")

        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/listings/de-DE').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/listings/en-US').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/listings/fr-FR').exists())

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/release-notes/de-DE/default.txt').text
        assertEquals('free german', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/release-notes/fr-FR/default.txt').text
        assertEquals('main', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/freeRelease/res/release-notes/en-US/default.txt').text
        assertEquals('free', content)

        executeConnection(connection, "generatePaidReleasePlayResources")

        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/paidRelease/res/release-notes/de-DE/default.txt').text
        assertEquals('paid german', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/paidRelease/res/release-notes/fr-FR/default.txt').text
        assertEquals('main', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/paidRelease/res/release-notes/en-US/default.txt').text
        assertEquals('paid english', content)

        cleanup()
    }

    @Test
    void testBuildTypeOverridesMain() {
        // language=gradle
        def connection = generateConnection("""
            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateDogfoodPlayResources")

        cleanup()

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/dogfood/res/release-notes/en-US/default.txt').text
        assertEquals('dogfood english', content)
    }

    @Test
    void testBuildTypeOverridesFlavor() {
        // language=gradle
        def connection = generateConnection("""
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
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generatePaidDogfoodPlayResources")

        cleanup()

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/paidDogfood/res/release-notes/en-US/default.txt').text
        assertEquals('dogfood english', content)
    }

    @Test
    void testVariantOverridesBuildType() {
        // language=gradle
        def connection = generateConnection("""
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
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateFreeDogfoodPlayResources")

        cleanup()

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/generated/gpp/freeDogfood/res/release-notes/en-US/default.txt').text
        assertEquals('free dogfood english', content)
    }

    @Test
    void multidimensionalResourcesAreNotOverwritten() {
        def originalReleaseNotes = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStagingDogfood/play/release-notes/en-US/default.txt').text
        def originalFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStagingDogfood/play/listings/en-US/full-description.txt').text

        // language=gradle
        def connection = generateConnection("""
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
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateFreeStagingDogfoodPlayResources")

        cleanup()

        def processedReleaseNotes = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingDogfood/res/release-notes/en-US/default.txt').text
        def processedFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingDogfood/res/listings/en-US/full-description.txt').text

        assertEquals(originalReleaseNotes, processedReleaseNotes)
        assertEquals(originalFullDescription, processedFullDescription)
    }

    @Test(expected = IllegalStateException)
    void invalidLocaleThrows() {
        // language=gradle
        def connection = generateConnection("""
            flavorDimensions 'pricing'

            productFlavors {
                invalidLocale { dimension 'pricing' }
            }
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateInvalidLocaleReleasePlayResources")

        cleanup()
    }

    @Test(expected = IllegalStateException)
    void fileInWrongDirThrows() {
        // language=gradle
        def connection = generateConnection("""
            flavorDimensions 'pricing'

            productFlavors {
                unknownFile { dimension 'pricing' }
            }
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateUnknownFileReleasePlayResources")

        cleanup()
    }

    @Test
    void multidimensionalFlavorsMerge() {
        def originalReleaseNotes = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStaging/play/release-notes/en-US/default.txt').text
        def originalFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStagingRelease/play/listings/en-US/full-description.txt').text

        // language=gradle
        def connection = generateConnection("""
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateFreeStagingReleasePlayResources")

        cleanup()

        def processedReleaseNotes = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/release-notes/en-US/default.txt').text
        def processedFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/listings/en-US/full-description.txt').text

        assertEquals(originalReleaseNotes, processedReleaseNotes)
        assertEquals(originalFullDescription, processedFullDescription)
    }

    @Test
    void flavorMerge() {
        def originalFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStagingRelease/play/listings/en-US/full-description.txt').text
        def originalShortDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/free/play/listings/en-US/short-description.txt').text

        // language=gradle
        def connection = generateConnection("""
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateFreeStagingReleasePlayResources")

        cleanup()

        def processedFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/listings/en-US/full-description.txt').text
        def processedShortDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/listings/en-US/short-description.txt').text

        assertEquals(originalFullDescription, processedFullDescription)
        assertEquals(originalShortDescription, processedShortDescription)
    }

    @Test
    void flavorDimensionOrderDeterminesConflictingFlavorWinner() {
        def originalReleaseNotes = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/prod/play/release-notes/en-US/default.txt').text

        // language=gradle
        def connection = generateConnection("""
            flavorDimensions 'server', 'pricing'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateProdFreeReleasePlayResources")

        cleanup()

        def processedReleaseNotes = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodFreeRelease/res/release-notes/en-US/default.txt').text

        assertEquals(originalReleaseNotes, processedReleaseNotes)
    }

    @Test
    void mainMerge() {
        def originalFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/freeStagingRelease/play/listings/en-US/full-description.txt').text
        def originalTitle = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/main/play/listings/en-US/title.txt').text

        // language=gradle
        def connection = generateConnection("""
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateFreeStagingReleasePlayResources")

        cleanup()

        def processedFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/listings/en-US/full-description.txt').text
        def processedTitle = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/freeStagingRelease/res/listings/en-US/title.txt').text

        assertEquals(originalFullDescription, processedFullDescription)
        assertEquals(originalTitle, processedTitle)
    }

    @Test
    void languageMerge() {
        def originalTitle = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/main/play/listings/en-US/title.txt').text
        def originalFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/main/play/listings/de-DE/full-description.txt').text
        def originalShortDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/staging/play/listings/en-US/short-description.txt').text

        // language=gradle
        def connection = generateConnection("""
            flavorDimensions 'pricing', 'server'

            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """)

        executeConnection(connection, "clean")
        executeConnection(connection, "generateProdStagingReleasePlayResources")

        cleanup()

        def processedTitle = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/de-DE/title.txt').text
        def processedFullDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/de-DE/full-description.txt').text
        def processedShortDescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/generated/gpp/prodStagingRelease/res/listings/de-DE/short-description.txt').text

        assertEquals(originalTitle, processedTitle)
        assertEquals(originalFullDescription, processedFullDescription)
        assertEquals(originalShortDescription, processedShortDescription)
    }
}
