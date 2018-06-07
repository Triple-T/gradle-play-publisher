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
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/en-US').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/fr-FR').exists())

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/en-US/whatsnew').text
        assertEquals('main', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/release/res/fr-FR/whatsnew').text
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
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/de-DE').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/en-US').exists())
        assertTrue(new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/fr-FR').exists())

        def content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/de-DE/whatsnew').text
        assertEquals('free german', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/fr-FR/whatsnew').text
        assertEquals('main', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/freeRelease/res/en-US/whatsnew').text
        assertEquals('main', content)

        project.tasks.generatePaidReleasePlayResources.execute()

        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/res/de-DE/whatsnew').text
        assertEquals('paid german', content)
        content = new File(TestHelper.FIXTURE_WORKING_DIR, 'build/outputs/play/paidRelease/res/fr-FR/whatsnew').text
        assertEquals('main', content)
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

    @Test
    void multidimensionalResourcesAreNotOverwritten() {
        def project = TestHelper.evaluatableProject()
        def originalWhatsnew = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/flavor1Flavor3BuildType1/play/en-US/whatsnew').text
        def originalFulldescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/flavor1Flavor3BuildType1/play/en-US/listing/fulldescription').text

        project.android {
            flavorDimensions 'diem1', 'diem2'

            productFlavors {
                flavor1 { dimension 'diem1' }
                flavor2 { dimension 'diem1' }
                flavor3 { dimension 'diem2' }
                flavor4 { dimension 'diem2' }
            }

            buildTypes {
                buildType1.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFlavor1Flavor3BuildType1PlayResources.execute()

        def processedWhatsnew = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/flavor1Flavor3BuildType1/res/en-US/whatsnew').text
        def processedFulldescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/flavor1Flavor3BuildType1/res/en-US/listing/fulldescription').text

        assertEquals(originalWhatsnew, processedWhatsnew)
        assertEquals(originalFulldescription, processedFulldescription)
    }

    @Test(expected = TaskExecutionException)
    void conflictingFlavorsThrow() {
        def project = TestHelper.evaluatableProject()

        project.android {
            flavorDimensions 'diem1', 'diem2'

            productFlavors {
                flavor1 { dimension 'diem1' }
                flavor2 { dimension 'diem1' }
                flavor3 { dimension 'diem2' }
                flavor4 { dimension 'diem2' }
            }

            buildTypes {
                buildType1.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFlavor1Flavor4BuildType1PlayResources.execute()
    }

    @Test(expected = TaskExecutionException)
    void invalidLocaleThrow() {
        def project = TestHelper.evaluatableProject()

        project.android {
            flavorDimensions 'diem1'

            productFlavors {
                invalidLocale { dimension 'diem1' }
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateInvalidLocaleReleasePlayResources.execute()
    }

    @Test
    void multidimensionalFlavorsMerge() {
        def project = TestHelper.evaluatableProject()
        def originalWhatsnew = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/flavor1Flavor3/play/en-US/whatsnew').text
        def originalFulldescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/flavor1Flavor3Release/play/en-US/listing/fulldescription').text

        project.android {
            flavorDimensions 'diem1', 'diem2'

            productFlavors {
                flavor1 { dimension 'diem1' }
                flavor2 { dimension 'diem1' }
                flavor3 { dimension 'diem2' }
                flavor4 { dimension 'diem2' }
            }

            buildTypes {
                buildType1.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFlavor1Flavor3ReleasePlayResources.execute()

        def processedWhatsnew = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/flavor1Flavor3Release/res/en-US/whatsnew').text
        def processedFulldescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/flavor1Flavor3Release/res/en-US/listing/fulldescription').text

        assertEquals(originalWhatsnew, processedWhatsnew)
        assertEquals(originalFulldescription, processedFulldescription)
    }

    @Test
    void flavorMerge() {
        def project = TestHelper.evaluatableProject()
        def originalFulldescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/flavor1Flavor3Release/play/en-US/listing/fulldescription').text
        def originalShortdescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/flavor1/play/en-US/listing/shortdescription').text

        project.android {
            flavorDimensions 'diem1', 'diem2'

            productFlavors {
                flavor1 { dimension 'diem1' }
                flavor2 { dimension 'diem1' }
                flavor3 { dimension 'diem2' }
                flavor4 { dimension 'diem2' }
            }

            buildTypes {
                buildType1.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFlavor1Flavor3ReleasePlayResources.execute()

        def processedFulldescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/flavor1Flavor3Release/res/en-US/listing/fulldescription').text
        def processedShortdescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/flavor1Flavor3Release/res/en-US/listing/shortdescription').text

        assertEquals(originalFulldescription, processedFulldescription)
        assertEquals(originalShortdescription, processedShortdescription)
    }

    @Test
    void mainMerge() {
        def project = TestHelper.evaluatableProject()
        def originalFulldescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/flavor1Flavor3Release/play/en-US/listing/fulldescription').text
        def originalTitle = new File(TestHelper.FIXTURE_WORKING_DIR,
                'src/main/play/en-US/listing/title').text

        project.android {
            flavorDimensions 'diem1', 'diem2'

            productFlavors {
                flavor1 { dimension 'diem1' }
                flavor2 { dimension 'diem1' }
                flavor3 { dimension 'diem2' }
                flavor4 { dimension 'diem2' }
            }

            buildTypes {
                buildType1.initWith(buildTypes.release)
            }
        }

        project.evaluate()

        project.tasks.clean.execute()
        project.tasks.generateFlavor1Flavor3ReleasePlayResources.execute()

        def processedFulldescription = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/flavor1Flavor3Release/res/en-US/listing/fulldescription').text
        def processedTitle = new File(TestHelper.FIXTURE_WORKING_DIR,
                'build/outputs/play/flavor1Flavor3Release/res/en-US/listing/title').text

        assertEquals(originalFulldescription, processedFulldescription)
        assertEquals(originalTitle, processedTitle)
    }

}
