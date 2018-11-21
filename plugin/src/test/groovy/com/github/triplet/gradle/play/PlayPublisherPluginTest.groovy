package com.github.triplet.gradle.play

import org.junit.Test

import static DependsOn.dependsOn
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat

class PlayPublisherPluginTest {

    @Test
    void testCreatesDefaultTask() {
        def project = TestHelper.evaluatableProject()
        project.evaluate()

        assertNotNull(project.tasks.publishRelease)
        assertEquals(project.tasks.publishReleaseApk.variant, project.android.applicationVariants[1])
    }

    @Test
    void testCreatesFlavorTasks() {
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

        assertNotNull(project.tasks.publishPaidRelease)
        assertNotNull(project.tasks.publishFreeRelease)

        assertEquals(project.tasks.publishFreeReleaseApk.variant, project.android.applicationVariants[3])
        assertEquals(project.tasks.publishPaidReleaseApk.variant, project.android.applicationVariants[1])
    }

    @Test
    void testDefaultTrack() {
        def project = TestHelper.evaluatableProject()
        project.evaluate()

        assertEquals('internal', project.extensions.findByName('play').track)
    }

    @Test
    void testTrack() {
        def project = TestHelper.evaluatableProject()

        project.play {
            track 'production'
        }

        project.evaluate()

        assertEquals('production', project.extensions.findByName('play').track)
    }

    @Test(expected = IllegalArgumentException.class)
    void testThrowsOnInvalidTrack() {
        def project = TestHelper.evaluatableProject()

        project.play {
            track 'gamma'
        }
    }

    @Test
    void testUserFraction() {
        def project = TestHelper.evaluatableProject()

        project.play {
            userFraction 0.1
        }

        project.evaluate()

        assertEquals(0.1, project.extensions.findByName('play').userFraction, 0)
    }

    @Test
    void testPublishListingTask() {
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

        assertNotNull(project.tasks.publishFreeReleaseListing)
        assertNotNull(project.tasks.publishPaidReleaseListing)
    }

    @Test
    void testJsonFileBackwardsCompatibility() {
        def project = TestHelper.evaluatableProject()

        project.play {
            serviceAccountCredentials new File('key.json')
        }

        project.evaluate()

        assertEquals('key.json', project.extensions.play.serviceAccountCredentials.name)
    }

    @Test
    void testPlayAccountBackwardsCompatibility() {
        def project = TestHelper.evaluatableProject()

        project.play {
            serviceAccountEmail = 'service-account@test.com'
            serviceAccountCredentials = new File('key.p12')
        }

        project.evaluate()

        assertEquals('service-account@test.com', project.extensions.play.serviceAccountEmail)
        assertEquals(new File('key.p12'), project.extensions.play.serviceAccountCredentials)
    }

    @Test
    void testPlaySigningConfigs() {
        def project = TestHelper.evaluatableProject()

        project.android {
            playConfigs {
                free {
                    serviceAccountCredentials = project.file('secret.pk12')
                    serviceAccountEmail = 'first-mail@exmaple.com'
                }
                paid {
                    serviceAccountCredentials = project.file('another-secret.pk12')
                    serviceAccountEmail = 'another-mail@exmaple.com'
                }
            }

            flavorDimensions 'pricing'

            productFlavors {
                defaultFlavor {
                    dimension 'pricing'
                }
                free {
                    dimension 'pricing'
                }
                paid {
                    dimension 'pricing'
                }
            }
        }
        project.play {
            serviceAccountCredentials = project.file('first-secret.pk12')
            serviceAccountEmail = 'default@exmaple.com'
        }
        project.evaluate()

        assertEquals('default@exmaple.com', project.tasks.bootstrapDefaultFlavorReleasePlayResources.extension.serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', project.tasks.bootstrapFreeReleasePlayResources.extension.serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', project.tasks.bootstrapPaidReleasePlayResources.extension.serviceAccountEmail)

        assertEquals('default@exmaple.com', project.tasks.publishDefaultFlavorReleaseApk.extension.serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', project.tasks.publishFreeReleaseApk.extension.serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', project.tasks.publishPaidReleaseApk.extension.serviceAccountEmail)

        assertEquals('default@exmaple.com', project.tasks.publishDefaultFlavorReleaseListing.extension.serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', project.tasks.publishFreeReleaseListing.extension.serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', project.tasks.publishPaidReleaseListing.extension.serviceAccountEmail)
    }

    @Test
    void testPlaySigningConfigsDimensions() {
        def project = TestHelper.evaluatableProject()

        project.android {

            flavorDimensions "mode", "variant"

            playConfigs {
                free {
                    serviceAccountCredentials = project.file('secret.pk12')
                    serviceAccountEmail = 'free@exmaple.com'
                }
                paid {
                    serviceAccountCredentials = project.file('another-secret.pk12')
                    serviceAccountEmail = 'paid@exmaple.com'
                }
            }

            productFlavors {
                demo {
                    dimension = "mode"
                }
                production {
                    dimension = "mode"
                }
                free {
                    dimension = "variant"
                }
                paid {
                    dimension = "variant"
                }
            }
        }
        project.evaluate()

        assertEquals('free@exmaple.com', project.tasks.bootstrapDemoFreeReleasePlayResources.extension.serviceAccountEmail)
        assertEquals('paid@exmaple.com', project.tasks.bootstrapDemoPaidReleasePlayResources.extension.serviceAccountEmail)
        assertEquals('free@exmaple.com', project.tasks.bootstrapProductionFreeReleasePlayResources.extension.serviceAccountEmail)
        assertEquals('paid@exmaple.com', project.tasks.bootstrapProductionPaidReleasePlayResources.extension.serviceAccountEmail)
    }

    @Test
    void testNoProductFlavors() {
        def project = TestHelper.evaluatableProject()

        project.play {
            serviceAccountCredentials = project.file('first-secret.pk12')
            serviceAccountEmail = 'default@exmaple.com'
        }
        project.evaluate()

        assertEquals('default@exmaple.com', project.tasks.bootstrapReleasePlayResources.extension.serviceAccountEmail)
        assertEquals('default@exmaple.com', project.tasks.publishReleaseApk.extension.serviceAccountEmail)
        assertEquals('default@exmaple.com', project.tasks.publishReleaseListing.extension.serviceAccountEmail)
    }

    @Test
    void allTasksExist_AndDependOnBaseTasks_WithNoProductFlavor() {
        def project = TestHelper.evaluatableProject()
        project.evaluate()

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapReleasePlayResources'))
        assertThat(project.tasks.publish, dependsOn('publishRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishReleaseListing'))
    }

    @Test
    void allTasksExist_AndDependOnBaseTasks_ForAllProductFlavor() {
        def project = TestHelper.evaluatableProject()

        project.android {
            flavorDimensions "mode", "variant"

            productFlavors {
                demo {
                    dimension = "mode"
                }
                production {
                    dimension = "mode"
                }
                free {
                    dimension = "variant"
                }
                paid {
                    dimension = "variant"
                }
            }

        }
        project.evaluate()

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapDemoFreeReleasePlayResources'))
        assertThat(project.tasks.publish, dependsOn('publishDemoFreeRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishDemoFreeReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishDemoFreeReleaseListing'))

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapDemoPaidReleasePlayResources'))
        assertThat(project.tasks.publish, dependsOn('publishDemoPaidRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishDemoPaidReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishDemoPaidReleaseListing'))

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapProductionFreeReleasePlayResources'))
        assertThat(project.tasks.publish, dependsOn('publishProductionFreeRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishProductionFreeReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishProductionFreeReleaseListing'))

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapProductionPaidReleasePlayResources'))
        assertThat(project.tasks.publish, dependsOn('publishProductionPaidRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishProductionPaidReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishProductionPaidReleaseListing'))
    }

    @Test
    void signedBuildsCanBeAssembledWithoutCredsWhenResStratNotAuto() {
        def project = TestHelper.evaluatableProject()
        project.evaluate()

        def processTask = project.tasks.processReleaseMetadata
        processTask.execute() // Hack: we should be using the GradleRunner to run a real build

        assertFalse(processTask.didWork)
    }
}
