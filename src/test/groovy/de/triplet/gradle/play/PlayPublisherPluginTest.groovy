package de.triplet.gradle.play

import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Ignore
import org.junit.Test

import static de.triplet.gradle.play.DependsOn.dependsOn
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.fail

class PlayPublisherPluginTest {

    @Test(expected = PluginApplicationException.class)
    void testThrowsOnLibraryProjects() {
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.android.library'
        project.apply plugin: 'com.github.triplet.play'
    }

    @Test
    void testCreatesDefaultTask() {
        def project = TestHelper.evaluatableProject()
        project.evaluate()

        assertNotNull(project.tasks.publishRelease)
        assertEquals(project.tasks.publishApkRelease.variant, project.android.applicationVariants[1])
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

        assertEquals(project.tasks.publishApkFreeRelease.variant, project.android.applicationVariants[3])
        assertEquals(project.tasks.publishApkPaidRelease.variant, project.android.applicationVariants[1])
    }

    @Test
    void testDefaultTrack() {
        def project = TestHelper.evaluatableProject()
        project.evaluate()

        assertEquals('alpha', project.extensions.findByName('play').track)
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

        assertNotNull(project.tasks.publishListingFreeRelease)
        assertNotNull(project.tasks.publishListingPaidRelease)
    }

    @Test
    void testNoSigningConfigGenerateTasks() {
        def project = TestHelper.noSigningConfigProject()

        project.evaluate()

        assertNotNull(project.tasks.bootstrapReleasePlayResources)
        assertNotNull(project.tasks.publishListingRelease)

        if (project.tasks.hasProperty('publishApkRelease') || project.tasks.hasProperty('publishRelease')) {
            fail()
        }
    }

    @Test
    void testJsonFileBackwardsCompatibility() {
        def project = TestHelper.evaluatableProject()

        project.play {
            jsonFile new File('key.json')
        }

        project.evaluate()

        assertEquals('key.json', project.extensions.play.jsonFile.name)
    }

    @Test
    void testPlayAccountBackwardsCompatibility() {
        def project = TestHelper.evaluatableProject()

        project.play {
            serviceAccountEmail = 'service-account@test.com'
            pk12File = new File('key.p12')
        }

        project.evaluate()

        project.extensions.play.serviceAccountEmail == 'service-account@test.com'
        project.extensions.play.pk12File = new File('key.p12')
    }

    @Test
    void testPlaySigningConfigs() {
        def project = TestHelper.evaluatableProject()

        project.android {
            playAccountConfigs {
                defaultAccountConfig {
                    serviceAccountEmail = 'default@exmaple.com'
                    pk12File = project.file('first-secret.pk12')
                }
                free {
                    serviceAccountEmail = 'first-mail@exmaple.com'
                    pk12File = project.file('secret.pk12')
                }
                paid {
                    serviceAccountEmail = 'another-mail@exmaple.com'
                    pk12File = project.file('another-secret.pk12')
                }
            }

            defaultConfig {
                playAccountConfig = playAccountConfigs.defaultAccountConfig
            }

            flavorDimensions 'pricing'

            productFlavors {
                defaultFlavor {
                    dimension 'pricing'
                }
                free {
                    dimension 'pricing'
                    playAccountConfig = playAccountConfigs.free
                }
                paid {
                    dimension 'pricing'
                    playAccountConfig = playAccountConfigs.paid
                }
            }
        }
        project.evaluate()

        assertEquals('default@exmaple.com', project.tasks.bootstrapDefaultFlavorReleasePlayResources.playAccountConfig.serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', project.tasks.bootstrapFreeReleasePlayResources.playAccountConfig.serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', project.tasks.bootstrapPaidReleasePlayResources.playAccountConfig.serviceAccountEmail)

        assertEquals('default@exmaple.com', project.tasks.publishApkDefaultFlavorRelease.playAccountConfig.serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', project.tasks.publishApkFreeRelease.playAccountConfig.serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', project.tasks.publishApkPaidRelease.playAccountConfig.serviceAccountEmail)

        assertEquals('default@exmaple.com', project.tasks.publishListingDefaultFlavorRelease.playAccountConfig.serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', project.tasks.publishListingFreeRelease.playAccountConfig.serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', project.tasks.publishListingPaidRelease.playAccountConfig.serviceAccountEmail)
    }

    @Test
    void testPlaySigningConfigsDimensions() {
        def project = TestHelper.evaluatableProject()

        project.android {

            flavorDimensions "mode", "variant"

            playAccountConfigs {
                free {
                    serviceAccountEmail = 'free@exmaple.com'
                    pk12File = project.file('secret.pk12')
                }
                paid {
                    serviceAccountEmail = 'paid@exmaple.com'
                    pk12File = project.file('another-secret.pk12')
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
                    playAccountConfig = playAccountConfigs.free
                }
                paid {
                    dimension = "variant"
                    playAccountConfig = playAccountConfigs.paid
                }
            }
        }
        project.evaluate()

        assertEquals('free@exmaple.com', project.tasks.bootstrapDemoFreeReleasePlayResources.playAccountConfig.serviceAccountEmail)
        assertEquals('paid@exmaple.com', project.tasks.bootstrapDemoPaidReleasePlayResources.playAccountConfig.serviceAccountEmail)
        assertEquals('free@exmaple.com', project.tasks.bootstrapProductionFreeReleasePlayResources.playAccountConfig.serviceAccountEmail)
        assertEquals('paid@exmaple.com', project.tasks.bootstrapProductionPaidReleasePlayResources.playAccountConfig.serviceAccountEmail)
    }

    @Test
    void testNoProductFlavors() {
        def project = TestHelper.evaluatableProject()

        project.android {
            playAccountConfigs {
                defaultAccountConfig {
                    serviceAccountEmail = 'default@exmaple.com'
                    pk12File = project.file('first-secret.pk12')
                }
            }

            defaultConfig {
                playAccountConfig = playAccountConfigs.defaultAccountConfig
            }
        }
        project.evaluate()

        assertEquals('default@exmaple.com', project.tasks.bootstrapReleasePlayResources.playAccountConfig.serviceAccountEmail)
        assertEquals('default@exmaple.com', project.tasks.publishApkRelease.playAccountConfig.serviceAccountEmail)
        assertEquals('default@exmaple.com', project.tasks.publishListingRelease.playAccountConfig.serviceAccountEmail)
    }

    @Ignore("These test is not plugin specific and failing with the latest Android Gradle plugin")
    @Test
    void testSplits() {
        def project = TestHelper.evaluatableProject()

        project.android {
            splits {
                abi {
                    enable true
                    reset()
                    include 'x86', 'armeabi-v7a', 'mips'
                }
            }
        }

        project.evaluate()

        assertThat(project.tasks.assembleRelease, dependsOn('assembleX86Release'))
        assertThat(project.tasks.assembleRelease, dependsOn('assembleArmeabi-v7aRelease'))
        assertThat(project.tasks.assembleRelease, dependsOn('assembleMipsRelease'))

        assertThat(project.tasks.publishApkRelease, dependsOn('assembleRelease'))
    }
}
