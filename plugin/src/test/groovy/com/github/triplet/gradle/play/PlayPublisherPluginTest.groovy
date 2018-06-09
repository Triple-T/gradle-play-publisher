package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.TrackType
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static DependsOn.dependsOn
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

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

        assertEquals('internal', project.extensions.findByName('play').track)
    }

    @Test
    void test_InternalTrackWithDraftStatus_Evaluates() {
        assertTrue(evaluateProjectWithTrackAndStatus(TrackType.INTERNAL, ReleaseStatus.DRAFT))
    }

    @Test
    void test_InternalTrackWithCompletedStatus_Evaluates() {
        assertTrue(evaluateProjectWithTrackAndStatus(TrackType.INTERNAL, ReleaseStatus.COMPLETED))
    }

    @Test(expected = ProjectConfigurationException)
    void test_InternalTrackWithInProgressStatus_Fails() {
        evaluateProjectWithTrackAndStatus(TrackType.INTERNAL, ReleaseStatus.IN_PROGRESS)
    }

    @Test(expected = ProjectConfigurationException)
    void test_InternalTrackWithHaltedStatus_Fails() {
        evaluateProjectWithTrackAndStatus(TrackType.INTERNAL, ReleaseStatus.HALTED)
    }

    @Test
    void test_AlphaTrackWithDraftStatus_Evaluates() {
        assertTrue(evaluateProjectWithTrackAndStatus(TrackType.ALPHA, ReleaseStatus.DRAFT))
    }

    @Test
    void test_AlphaTrackWithCompletedStatus_Evaluates() {
        assertTrue(evaluateProjectWithTrackAndStatus(TrackType.ALPHA, ReleaseStatus.COMPLETED))
    }

    @Test(expected = ProjectConfigurationException)
    void test_AlphaTrackWithInProgressStatus_Fails() {
        evaluateProjectWithTrackAndStatus(TrackType.ALPHA, ReleaseStatus.IN_PROGRESS)
    }

    @Test(expected = ProjectConfigurationException)
    void test_AlphaTrackWithHaltedStatus_Fails() {
        evaluateProjectWithTrackAndStatus(TrackType.ALPHA, ReleaseStatus.HALTED)
    }

    @Test
    void test_BetaTrackWithDraftStatus_Evaluates() {
        assertTrue(evaluateProjectWithTrackAndStatus(TrackType.BETA, ReleaseStatus.DRAFT))
    }

    @Test
    void test_BetaTrackWithCompletedStatus_Evaluates() {
        assertTrue(evaluateProjectWithTrackAndStatus(TrackType.BETA, ReleaseStatus.COMPLETED))
    }

    @Test(expected = ProjectConfigurationException)
    void test_BetaTrackWithInProgressStatus_Fails() {
        evaluateProjectWithTrackAndStatus(TrackType.BETA, ReleaseStatus.IN_PROGRESS)
    }

    @Test(expected = ProjectConfigurationException)
    void test_BetaTrackWithHaltedStatus_Fails() {
        evaluateProjectWithTrackAndStatus(TrackType.BETA, ReleaseStatus.HALTED)
    }

    @Test
    void test_ProductionTrackWithDraftStatus_Evaluates() {
        assertTrue(evaluateProjectWithTrackAndStatus(TrackType.PRODUCTION, ReleaseStatus.DRAFT))
    }

    @Test
    void test_ProductionTrackWithCompletedStatus_Evaluates() {
        assertTrue(evaluateProjectWithTrackAndStatus(TrackType.PRODUCTION, ReleaseStatus.COMPLETED))
    }

    @Test(expected = ProjectConfigurationException)
    void test_ProductionTrackWithInProgressStatus_Fails() {
        evaluateProjectWithTrackAndStatus(TrackType.PRODUCTION, ReleaseStatus.IN_PROGRESS)
    }

    @Test(expected = ProjectConfigurationException)
    void test_ProductionTrackWithHaltedStatus_Fails() {
        evaluateProjectWithTrackAndStatus(TrackType.PRODUCTION, ReleaseStatus.HALTED)
    }

    @Test(expected = ProjectConfigurationException)
    void test_RolloutTrackWithDraftStatus_Fails() {
        evaluateProjectWithTrackAndStatus(TrackType.ROLLOUT, ReleaseStatus.DRAFT)
    }

    @Test(expected = ProjectConfigurationException)
    void test_RolloutTrackWithCompletedStatus_Fails() {
        evaluateProjectWithTrackAndStatus(TrackType.ROLLOUT, ReleaseStatus.COMPLETED)
    }

    @Test
    void test_RolloutTrackWithHaltedStatus_Evaluates() {
        assertTrue(evaluateProjectWithTrackAndStatus(TrackType.ROLLOUT, ReleaseStatus.HALTED))
    }

    @Test
    void test_RolloutTrackWithInProgressStatus_Evaluates() {
        assertTrue(evaluateProjectWithTrackAndStatus(TrackType.ROLLOUT, ReleaseStatus.IN_PROGRESS))
    }

    @Test
    void test_ProjectWithTracksAndNoStatus_defaults() {
        assertEquals('completed',
                evaluateProjectWithTrack(TrackType.INTERNAL).extensions.findByName('play').releaseStatus)
        assertEquals('completed',
                evaluateProjectWithTrack(TrackType.ALPHA).extensions.findByName('play').releaseStatus)
        assertEquals('completed',
                evaluateProjectWithTrack(TrackType.BETA).extensions.findByName('play').releaseStatus)
        assertEquals('completed',
                evaluateProjectWithTrack(TrackType.PRODUCTION).extensions.findByName('play').releaseStatus)
        assertEquals('inProgress',
                evaluateProjectWithTrack(TrackType.ROLLOUT).extensions.findByName('play').releaseStatus)
    }

    private evaluateProjectWithTrackAndStatus(TrackType trackType, ReleaseStatus status) {
        def project = TestHelper.evaluatableProject()
        project.play {
            track trackType.publishedName
            releaseStatus status.publishedName
        }
        project.evaluate()
        return true
    }

    private evaluateProjectWithTrack(TrackType trackType) {
        def project = TestHelper.evaluatableProject()
        project.play {
            track trackType.publishedName
        }
        project.evaluate()
        return project
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

        assertEquals('service-account@test.com', project.extensions.play.serviceAccountEmail)
        assertEquals(new File('key.p12'), project.extensions.play.pk12File)
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

        assertEquals('default@exmaple.com', project.tasks.bootstrapDefaultFlavorReleasePlayResources.accountConfig.serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', project.tasks.bootstrapFreeReleasePlayResources.accountConfig.serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', project.tasks.bootstrapPaidReleasePlayResources.accountConfig.serviceAccountEmail)

        assertEquals('default@exmaple.com', project.tasks.publishApkDefaultFlavorRelease.accountConfig.serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', project.tasks.publishApkFreeRelease.accountConfig.serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', project.tasks.publishApkPaidRelease.accountConfig.serviceAccountEmail)

        assertEquals('default@exmaple.com', project.tasks.publishListingDefaultFlavorRelease.accountConfig.serviceAccountEmail)
        assertEquals('first-mail@exmaple.com', project.tasks.publishListingFreeRelease.accountConfig.serviceAccountEmail)
        assertEquals('another-mail@exmaple.com', project.tasks.publishListingPaidRelease.accountConfig.serviceAccountEmail)
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

        assertEquals('free@exmaple.com', project.tasks.bootstrapDemoFreeReleasePlayResources.accountConfig.serviceAccountEmail)
        assertEquals('paid@exmaple.com', project.tasks.bootstrapDemoPaidReleasePlayResources.accountConfig.serviceAccountEmail)
        assertEquals('free@exmaple.com', project.tasks.bootstrapProductionFreeReleasePlayResources.accountConfig.serviceAccountEmail)
        assertEquals('paid@exmaple.com', project.tasks.bootstrapProductionPaidReleasePlayResources.accountConfig.serviceAccountEmail)
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

        assertEquals('default@exmaple.com', project.tasks.bootstrapReleasePlayResources.accountConfig.serviceAccountEmail)
        assertEquals('default@exmaple.com', project.tasks.publishApkRelease.accountConfig.serviceAccountEmail)
        assertEquals('default@exmaple.com', project.tasks.publishListingRelease.accountConfig.serviceAccountEmail)
    }

    @Test
    void allTasksExist_AndDependOnBaseTasks_WithNoProductFlavor() {
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

        assertThat(project.tasks.bootstrapAll, dependsOn('bootstrapReleasePlayResources'))
        assertThat(project.tasks.publishAll, dependsOn('publishRelease'))
        assertThat(project.tasks.publishApkAll, dependsOn('publishApkRelease'))
        assertThat(project.tasks.publishListingAll, dependsOn('publishListingRelease'))
    }

    @Test
    void allTasksExist_AndDependOnBaseTasks_ForAllProductFlavor() {
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

        assertThat(project.tasks.bootstrapAll, dependsOn('bootstrapDemoFreeReleasePlayResources'))
        assertThat(project.tasks.publishAll, dependsOn('publishDemoFreeRelease'))
        assertThat(project.tasks.publishApkAll, dependsOn('publishApkDemoFreeRelease'))
        assertThat(project.tasks.publishListingAll, dependsOn('publishListingDemoFreeRelease'))

        assertThat(project.tasks.bootstrapAll, dependsOn('bootstrapDemoPaidReleasePlayResources'))
        assertThat(project.tasks.publishAll, dependsOn('publishDemoPaidRelease'))
        assertThat(project.tasks.publishApkAll, dependsOn('publishApkDemoPaidRelease'))
        assertThat(project.tasks.publishListingAll, dependsOn('publishListingDemoPaidRelease'))

        assertThat(project.tasks.bootstrapAll, dependsOn('bootstrapProductionFreeReleasePlayResources'))
        assertThat(project.tasks.publishAll, dependsOn('publishProductionFreeRelease'))
        assertThat(project.tasks.publishApkAll, dependsOn('publishApkProductionFreeRelease'))
        assertThat(project.tasks.publishListingAll, dependsOn('publishListingProductionFreeRelease'))

        assertThat(project.tasks.bootstrapAll, dependsOn('bootstrapProductionPaidReleasePlayResources'))
        assertThat(project.tasks.publishAll, dependsOn('publishProductionPaidRelease'))
        assertThat(project.tasks.publishApkAll, dependsOn('publishApkProductionPaidRelease'))
        assertThat(project.tasks.publishListingAll, dependsOn('publishListingProductionPaidRelease'))
    }
}
