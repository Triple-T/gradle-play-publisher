package de.triplet.gradle.play

import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

class PlayPublisherPluginTest {

    @Test(expected = PluginApplicationException.class)
    public void testThrowsOnLibraryProjects() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.android.library'
        project.apply plugin: 'com.github.triplet.play'
    }

    @Test
    public void testCreatesDefaultTask() {
        Project project = TestHelper.evaluatableProject()
        project.evaluate()

        assertNotNull(project.tasks.publishRelease)
        assertEquals(project.tasks.publishApkRelease.variant, project.android.applicationVariants[1])
    }

    @Test
    public void testCreatesFlavorTasks() {
        Project project = TestHelper.evaluatableProject()

        project.android.productFlavors {
            free
            paid
        }

        project.evaluate()

        assertNotNull(project.tasks.publishPaidRelease)
        assertNotNull(project.tasks.publishFreeRelease)

        assertEquals(project.tasks.publishApkFreeRelease.variant, project.android.applicationVariants[3])
        assertEquals(project.tasks.publishApkPaidRelease.variant, project.android.applicationVariants[1])
    }

    @Test
    public void testDefaultTrack() {
        Project project = TestHelper.evaluatableProject()
        project.evaluate()

        assertEquals('alpha', project.extensions.findByName("play").track)
    }

    @Test
    public void testTrack() {
        Project project = TestHelper.evaluatableProject()

        project.play {
            track 'production'
        }

        project.evaluate()

        assertEquals('production', project.extensions.findByName("play").track)
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsOnInvalidTrack() {
        Project project = TestHelper.evaluatableProject()

        project.play {
            track 'gamma'
        }
    }

    @Test
    public void testUserFraction() {
        Project project = TestHelper.evaluatableProject()

        project.play {
            userFraction 0.1
        }

        project.evaluate()

        assertEquals(0.1, project.extensions.findByName("play").userFraction, 0)
    }

    @Test
    public void testPublishListingTask() {
        Project project = TestHelper.evaluatableProject()

        project.android.productFlavors {
            free
            paid
        }

        project.evaluate()

        assertNotNull(project.tasks.publishListingFreeRelease)
        assertNotNull(project.tasks.publishListingPaidRelease)
    }

    @Test
    public void testNoSigningConfigGenerateTasks() {
        Project project = TestHelper.noSigningConfigProject()

        project.evaluate()

        assertNotNull(project.tasks.bootstrapReleasePlayResources)
        assertNotNull(project.tasks.publishListingRelease)

        if (project.tasks.hasProperty('publishApkRelease') || project.tasks.hasProperty('publishRelease')) {
            fail()
        }
    }

    @Test
    public void testPlaySigningConfigs() {
        Project project = TestHelper.evaluatableProject()

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

            productFlavors {
                defaultFlavor {

                }
                free {
                    playAccountConfig = playAccountConfigs.free
                }
                paid {
                    playAccountConfig = playAccountConfigs.paid
                }
            }
        }
        project.evaluate()

        assert project.android.productFlavors.defaultFlavor.playAccountConfig.serviceAccountEmail == 'default@exmaple.com'
        assert project.android.productFlavors.free.playAccountConfig.serviceAccountEmail == 'first-mail@exmaple.com'
        assert project.android.productFlavors.paid.playAccountConfig.serviceAccountEmail == 'another-mail@exmaple.com'
    }

}
