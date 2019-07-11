package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.tasks.internal.PlayPublishTaskBase
import org.gradle.api.ProjectConfigurationException
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

import java.lang.reflect.Field

import static DependsOn.dependsOn
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat

class PlayPublisherPluginTest {

    @Test
    void testCreatesDefaultTask() {
        def project = TestHelper.evaluatableProject()
        project.evaluate()

        assertNotNull(project.tasks.publishRelease)
        assertEquals(var(project.tasks.publishReleaseApk), project.android.applicationVariants[1])
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

        assertEquals(var(project.tasks.publishFreeReleaseApk), project.android.applicationVariants[3])
        assertEquals(var(project.tasks.publishPaidReleaseApk), project.android.applicationVariants[1])
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

        assertEquals('default@exmaple.com', ext(project.tasks.bootstrapDefaultFlavorRelease).serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', ext(project.tasks.bootstrapFreeRelease).serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', ext(project.tasks.bootstrapPaidRelease).serviceAccountEmail)

        assertEquals('default@exmaple.com', ext(project.tasks.publishDefaultFlavorReleaseApk).serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', ext(project.tasks.publishFreeReleaseApk).serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', ext(project.tasks.publishPaidReleaseApk).serviceAccountEmail)

        assertEquals('default@exmaple.com', ext(project.tasks.publishDefaultFlavorReleaseListing).serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', ext(project.tasks.publishFreeReleaseListing).serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', ext(project.tasks.publishPaidReleaseListing).serviceAccountEmail)
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

        assertEquals('free@exmaple.com', ext(project.tasks.bootstrapDemoFreeRelease).serviceAccountEmail)
        assertEquals('paid@exmaple.com', ext(project.tasks.bootstrapDemoPaidRelease).serviceAccountEmail)
        assertEquals('free@exmaple.com', ext(project.tasks.bootstrapProductionFreeRelease).serviceAccountEmail)
        assertEquals('paid@exmaple.com', ext(project.tasks.bootstrapProductionPaidRelease).serviceAccountEmail)
    }

    @Test
    void testNoProductFlavors() {
        def project = TestHelper.evaluatableProject()

        project.play {
            serviceAccountCredentials = project.file('first-secret.pk12')
            serviceAccountEmail = 'default@exmaple.com'
        }
        project.evaluate()

        assertEquals('default@exmaple.com', ext(project.tasks.bootstrapRelease).serviceAccountEmail)
        assertEquals('default@exmaple.com', ext(project.tasks.publishReleaseApk).serviceAccountEmail)
        assertEquals('default@exmaple.com', ext(project.tasks.publishReleaseListing).serviceAccountEmail)
    }

    @Test
    void allTasksExist_AndDependOnBaseTasks_WithNoProductFlavor() {
        def project = TestHelper.evaluatableProject()
        project.evaluate()

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapRelease'))
        assertThat(project.tasks.publishAll, dependsOn('publishRelease'))
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

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapDemoFreeRelease'))
        assertThat(project.tasks.publishAll, dependsOn('publishDemoFreeRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishDemoFreeReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishDemoFreeReleaseListing'))

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapDemoPaidRelease'))
        assertThat(project.tasks.publishAll, dependsOn('publishDemoPaidRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishDemoPaidReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishDemoPaidReleaseListing'))

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapProductionFreeRelease'))
        assertThat(project.tasks.publishAll, dependsOn('publishProductionFreeRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishProductionFreeReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishProductionFreeReleaseListing'))

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapProductionPaidRelease'))
        assertThat(project.tasks.publishAll, dependsOn('publishProductionPaidRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishProductionPaidReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishProductionPaidReleaseListing'))
    }

    @Test(expected = ProjectConfigurationException.class)
    void projectEvaluationFailsWithNoCreds() {
        def project = TestHelper.evaluatableProject()

        project.play {
            serviceAccountCredentials = null
        }

        project.evaluate()
    }

    @Test
    void projectEvaluationSucceedsWithVariantSpecificCreds() {
        def project = TestHelper.evaluatableProject()

        project.play {
            enabled = false
            serviceAccountCredentials = null
        }
        project.android {
            flavorDimensions('d')
            productFlavors {
                f1 {}
                f2 {}
            }

            playConfigs {
                f1 {
                    enabled = true
                    serviceAccountCredentials = project.file('fake.json')
                }
            }
        }

        project.evaluate()
    }

    @Test
    void signedBuildsCanBeAssembledWithoutCredsWhenResStratNotAuto() {
        def result = TestHelper.execute("", "processReleaseMetadata")

        assertEquals(TaskOutcome.SKIPPED, result.task(":processReleaseMetadata").outcome)
    }

    private PlayPublisherExtension ext(PlayPublishTaskBase task) {
        Class c = task.class
        while (c != PlayPublishTaskBase.class) c = c.superclass
        Field f = c.getDeclaredField("extension")
        f.setAccessible(true)
        return f.get(task) as PlayPublisherExtension
    }

    private Object var(PlayPublishTaskBase task) {
        Class c = task.class
        while (c != PlayPublishTaskBase.class) c = c.superclass
        Field f = c.getDeclaredField("variant")
        f.setAccessible(true)
        return f.get(task)
    }
}
