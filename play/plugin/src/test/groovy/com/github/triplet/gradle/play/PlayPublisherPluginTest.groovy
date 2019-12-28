package com.github.triplet.gradle.play

import com.github.triplet.gradle.common.utils.IoKt
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.lang.reflect.Field

import static DependsOn.dependsOn
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat

// TODO(#709): rewrite this stuff into non-garbage Kotlin
class PlayPublisherPluginTest {
    @Test
    void testCreatesDefaultTask() {
        def project = evaluatableProject()
        project.evaluate()

        assertNotNull(project.tasks.publishRelease)
        assertEquals(var(project.tasks.publishReleaseApk), project.android.applicationVariants[1])
    }

    @Test
    void testCreatesFlavorTasks() {
        def project = evaluatableProject()

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

        assertEquals(
                var(project.tasks.publishFreeReleaseApk),
                project.android.applicationVariants.find { it.name == 'freeRelease' })
        assertEquals(
                var(project.tasks.publishPaidReleaseApk),
                project.android.applicationVariants.find { it.name == 'paidRelease' })
    }

    @Test
    void testDefaultTrack() {
        def project = evaluatableProject()
        project.evaluate()

        assertEquals('internal', project.extensions.findByName('play').track)
    }

    @Test
    void testTrack() {
        def project = evaluatableProject()

        project.play {
            track 'production'
        }

        project.evaluate()

        assertEquals('production', project.extensions.findByName('play').track)
    }

    @Test
    void testUserFraction() {
        def project = evaluatableProject()

        project.play {
            userFraction 0.1
        }

        project.evaluate()

        assertEquals(0.1, project.extensions.findByName('play').userFraction, 0)
    }

    @Test
    void testPublishListingTask() {
        def project = evaluatableProject()

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
        def project = evaluatableProject()

        project.play {
            serviceAccountCredentials new File('key.json')
        }

        project.evaluate()

        assertEquals('key.json', project.extensions.play.serviceAccountCredentials.name)
    }

    @Test
    void testPlayAccountBackwardsCompatibility() {
        def project = evaluatableProject()

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
        def project = evaluatableProject()

        project.android {
            playConfigs {
                free {
                    serviceAccountCredentials = project.file('secret.pk12')
                    serviceAccountEmail = 'first-mail@example.com'
                }
                paid {
                    serviceAccountCredentials = project.file('another-secret.pk12')
                    serviceAccountEmail = 'another-mail@example.com'
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
            serviceAccountEmail = 'default@example.com'
        }
        project.evaluate()

        assertEquals(
                'default@example.com',
                project.tasks.bootstrapDefaultFlavorRelease.extension.serviceAccountEmail
        )
        assertEquals(
                'first-mail@example.com',
                project.tasks.bootstrapFreeRelease.extension.serviceAccountEmail
        )
        assertEquals(
                'another-mail@example.com',
                project.tasks.bootstrapPaidRelease.extension.serviceAccountEmail
        )

        assertEquals(
                'default@example.com',
                project.tasks.publishDefaultFlavorReleaseApk.extension.serviceAccountEmail
        )
        assertEquals(
                'first-mail@example.com',
                project.tasks.publishFreeReleaseApk.extension.serviceAccountEmail
        )
        assertEquals(
                'another-mail@example.com',
                project.tasks.publishPaidReleaseApk.extension.serviceAccountEmail
        )

        assertEquals(
                'default@example.com',
                project.tasks.publishDefaultFlavorReleaseListing.extension.serviceAccountEmail
        )
        assertEquals(
                'first-mail@example.com',
                project.tasks.publishFreeReleaseListing.extension.serviceAccountEmail
        )
        assertEquals(
                'another-mail@example.com',
                project.tasks.publishPaidReleaseListing.extension.serviceAccountEmail
        )
    }

    @Test
    void testPlaySigningConfigsDimensions() {
        def project = evaluatableProject()

        project.android {

            flavorDimensions "mode", "variant"

            playConfigs {
                free {
                    serviceAccountCredentials = project.file('secret.pk12')
                    serviceAccountEmail = 'free@example.com'
                }
                paid {
                    serviceAccountCredentials = project.file('another-secret.pk12')
                    serviceAccountEmail = 'paid@example.com'
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

        assertEquals(
                'free@example.com',
                project.tasks.bootstrapDemoFreeRelease.extension.serviceAccountEmail
        )
        assertEquals(
                'paid@example.com',
                project.tasks.bootstrapDemoPaidRelease.extension.serviceAccountEmail
        )
        assertEquals(
                'free@example.com',
                project.tasks.bootstrapProductionFreeRelease.extension.serviceAccountEmail
        )
        assertEquals(
                'paid@example.com',
                project.tasks.bootstrapProductionPaidRelease.extension.serviceAccountEmail
        )
    }

    @Test
    void testNoProductFlavors() {
        def project = evaluatableProject()

        project.play {
            serviceAccountCredentials = project.file('first-secret.pk12')
            serviceAccountEmail = 'default@example.com'
        }
        project.evaluate()

        assertEquals(
                'default@example.com',
                project.tasks.bootstrapRelease.extension.serviceAccountEmail
        )
        assertEquals(
                'default@example.com',
                project.tasks.publishReleaseApk.extension.serviceAccountEmail
        )
        assertEquals(
                'default@example.com',
                project.tasks.publishReleaseListing.extension.serviceAccountEmail
        )
    }

    @Test
    void allTasksExist_AndDependOnBaseTasks_WithNoProductFlavor() {
        def project = evaluatableProject()
        project.evaluate()

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapRelease'))
        assertThat(project.tasks.publish, dependsOn('publishRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishReleaseListing'))
    }

    @Test
    void allTasksExist_AndDependOnBaseTasks_ForAllProductFlavor() {
        def project = evaluatableProject()

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
        assertThat(project.tasks.publish, dependsOn('publishDemoFreeRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishDemoFreeReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishDemoFreeReleaseListing'))

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapDemoPaidRelease'))
        assertThat(project.tasks.publish, dependsOn('publishDemoPaidRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishDemoPaidReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishDemoPaidReleaseListing'))

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapProductionFreeRelease'))
        assertThat(project.tasks.publish, dependsOn('publishProductionFreeRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishProductionFreeReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishProductionFreeReleaseListing'))

        assertThat(project.tasks.bootstrap, dependsOn('bootstrapProductionPaidRelease'))
        assertThat(project.tasks.publish, dependsOn('publishProductionPaidRelease'))
        assertThat(project.tasks.publishApk, dependsOn('publishProductionPaidReleaseApk'))
        assertThat(project.tasks.publishListing, dependsOn('publishProductionPaidReleaseListing'))
    }

    @Test(expected = ProjectConfigurationException.class)
    void projectEvaluationFailsWithNoCreds() {
        def project = evaluatableProject()

        project.play {
            serviceAccountCredentials = null
        }

        project.evaluate()
    }

    @Test
    void projectEvaluationSucceedsWithVariantSpecificCreds() {
        def project = evaluatableProject()

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

    // TODO(asaveau): too much effort to fix right now
    @Ignore
    @Test
    void signedBuildsCanBeAssembledWithoutCredsWhenResStratNotAuto() {
        def result = execute("", "processReleaseMetadata")

        assertEquals(TaskOutcome.SKIPPED, result.task(":processReleaseMetadata").outcome)
    }

    private Object var(PublishTaskBase task) {
        Class c = task.class
        while (c != PublishTaskBase.class) c = c.superclass
        Field f = c.getDeclaredField("variant")
        f.setAccessible(true)
        return f.get(task)
    }

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder()

    private File getAppFolder() {
        return new File(tempDir.root, "app")
    }

    @Before
    void initTestResources() {
        FileUtils.copyDirectory(
                new File("src/test/fixtures/app"),
                getAppFolder()
        )
        FileUtils.copyDirectory(
                new File("src/test/fixtures/GenerateResourcesIntegrationTest"),
                getAppFolder()
        )
    }

    private Project fixtureProject() {
        def project = ProjectBuilder.builder().withProjectDir(getAppFolder()).build()

        def base = new File(project.buildDir, "outputs/apk")
        IoKt.safeCreateNewFile(new File(base, "release/test-release.apk")).write("")
        IoKt.safeCreateNewFile(new File(base, "paid/release/test-paid-release.apk")).write("")

        return project
    }

    private Project evaluatableProject() {
        def project = fixtureProject()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'com.github.triplet.play'
        project.android {
            compileSdkVersion 28

            defaultConfig {
                versionCode 1
                versionName '1.0'
                minSdkVersion 28
                targetSdkVersion 28
            }

            buildTypes {
                release {
                    signingConfig signingConfigs.debug
                }
            }
        }
        project.play {
            serviceAccountCredentials = new File("fake.json")
        }

        return project
    }

    private BuildResult execute(String androidConfig, String... tasks) {
        execute(androidConfig, false, tasks)
    }

    private BuildResult execute(String androidConfig, boolean expectFailure, String... tasks) {
        new File(getAppFolder(), "build.gradle").write("""
        buildscript {
            repositories {
                google()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:3.6.0-alpha11'
                classpath files('../../../../build/libs/plugin-${System.getProperty("VERSION_NAME")}.jar')

                // Manually define transitive dependencies for our plugin since we don't have the
                // POM to fetch them for us
                classpath('com.google.apis:google-api-services-androidpublisher:v3-rev46-1.25.0')
            }
        }

        apply plugin: 'com.android.application'
        apply plugin: 'com.github.triplet.play'

        android {
            compileSdkVersion 28

            defaultConfig {
                applicationId "com.example.publisher"
                minSdkVersion 21
                targetSdkVersion 28
                versionCode 1
                versionName "1.0"
            }

            ${androidConfig}
        }

        play {
            serviceAccountCredentials = file('some-file.json')
        }
        """)

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(getAppFolder())
                .withArguments(tasks)

        if (expectFailure) return runner.buildAndFail() else return runner.build()
    }
}
