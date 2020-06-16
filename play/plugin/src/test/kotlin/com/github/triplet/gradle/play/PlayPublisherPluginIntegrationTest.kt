package com.github.triplet.gradle.play

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class PlayPublisherPluginIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play." +
            "PlayPublisherPluginIntegrationTest.installFactories()"

    @Test
    fun `Error is thrown if AGP is not applied`() {
        // language=gradle
        File(appDir, "build.gradle").writeText("""
            plugins {
                id 'com.github.triplet.play'
            }

            play {
                serviceAccountCredentials = file('creds.json')
            }
        """)

        val result = executeGradle(true) {
            withArguments("help")
        }

        assertThat(result.output).contains("Gradle Play Publisher cannot be configured.")
    }

    @Test
    fun `Maven publish plugin can be applied`() {
        // language=gradle
        val config = """
            apply plugin: 'maven-publish'
        """

        execute(config, "help")
    }

    @Test
    fun `Debuggable build types are ignored`() {
        // language=gradle
        val config = """
            buildTypes {
                release {
                    debuggable true
                }
            }
        """.withAndroidBlock()

        val result = execute(config, "tasks", "--group", "publishing")

        assertThat(result.output).doesNotContain("publishRelease")
    }

    @Test
    fun `Disabled build types are ignored`() {
        // language=gradle
        val config = """
            variantFilter { variant ->
                variant.setIgnore(true)
            }
        """.withAndroidBlock()

        val result = execute(config, "tasks", "--group", "publishing")

        assertThat(result.output).doesNotContain("publishRelease")
    }

    @Test
    fun `Groovy configuration options`() {
        // language=gradle
        val config = """
            play {
                enabled = true
                serviceAccountCredentials = file('creds.json')
                defaultToAppBundles = false
                commit = true
                fromTrack = 'from'
                track = 'track'
                promoteTrack = 'promote'
                userFraction = 0.5d
                updatePriority = 3
                releaseStatus = ReleaseStatus.COMPLETED
                releaseName = 'name'
                resolutionStrategy = ResolutionStrategy.AUTO
                artifactDir = file('.')

                retain {
                    artifacts = [1l, 2l, 3l]
                    mainObb = 8
                    patchObb = 8
                }
            }
        """

        execute(config, "help")
    }

    @Test
    fun `Kotlin configuration options`() {
        // language=gradle
        File(appDir, "build.gradle.kts").writeText("""
            import com.github.triplet.gradle.androidpublisher.ReleaseStatus
            import com.github.triplet.gradle.androidpublisher.ResolutionStrategy

            plugins {
                id("android")
                id("com.github.triplet.play")
            }

            android {
                compileSdkVersion(28)

                defaultConfig {
                    applicationId = "com.example.publisher"
                    minSdkVersion(21)
                    targetSdkVersion(28)
                    versionCode = 1
                    versionName = "1.0"
                }
            }

            play {
                enabled.set(true)
                serviceAccountCredentials.set(file("creds.json"))
                defaultToAppBundles.set(false)
                commit.set(true)
                fromTrack.set("from")
                track.set("track")
                promoteTrack.set("promote")
                userFraction.set(0.5)
                updatePriority.set(3)
                releaseStatus.set(ReleaseStatus.COMPLETED)
                releaseName.set("name")
                resolutionStrategy.set(ResolutionStrategy.AUTO)
                artifactDir.set(file("."))

                retain {
                    artifacts.set(listOf(1, 2, 3))
                    mainObb.set(8)
                    patchObb.set(8)
                }
            }
        """)

        executeGradle(false) {
            withArguments("help")
        }
    }

    @Test
    fun `Disabled GPP variant is ignored`() {
        // language=gradle
        val config = """
            playConfigs {
                release {
                    enabled.set(false)
                }
            }
        """.withAndroidBlock()

        val result = execute(config, "tasks", "--group", "publishing")

        assertThat(result.output).doesNotContain("publishRelease")
    }

    @Test
    fun `Internal sharing tasks are available for debug build types`() {
        val result = execute("", "tasks", "--group", "publishing")

        assertThat(result.output).contains("publishRelease")
        assertThat(result.output).doesNotContain("publishDebug")
        assertThat(result.output).contains("uploadRelease")
        assertThat(result.output).contains("uploadDebug")
        assertThat(result.output).contains("installRelease")
        assertThat(result.output).contains("installDebug")
    }

    @Test
    fun `No creds fails`() {
        // language=gradle
        File(appDir, "build.gradle").writeText("""
            plugins {
                id 'com.android.application'
                id 'com.github.triplet.play'
            }

            android {
                compileSdkVersion 28

                defaultConfig {
                    applicationId "com.example.publisher"
                    minSdkVersion 21
                    targetSdkVersion 28
                    versionCode 1
                    versionName "1.0"
                }
            }
        """)

        val result = executeGradle(true) {
            withArguments("help")
        }

        assertThat(result.output).contains("No credentials specified.")
    }

    @Test
    fun `Credentials can be specified from environment variable`() {
        // language=gradle
        File(appDir, "build.gradle").writeText("""
            plugins {
                id 'com.android.application'
                id 'com.github.triplet.play'
            }

            android {
                compileSdkVersion 28

                defaultConfig {
                    applicationId "com.example.publisher"
                    minSdkVersion 21
                    targetSdkVersion 28
                    versionCode 1
                    versionName "1.0"
                }
            }
        """)

        executeGradle(false) {
            withArguments("help")
            withEnvironment(mapOf(PlayPublisher.CREDENTIAL_ENV_VAR to "fake-creds"))
        }
    }

    @Test
    fun `Variant specific lifecycle task publishes APKs by default`() {
        val result = executeExpectingFailure("", "publishRelease")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")).isNull()
    }

    @Test
    fun `Variant specific lifecycle task publishes App Bundle when specified`() {
        // language=gradle
        val config = """
            play {
                defaultToAppBundles = true
            }
        """

        val result = executeExpectingFailure(config, "publishRelease")

        assertThat(result.task(":publishReleaseApk")).isNull()
        assertThat(result.task(":publishReleaseBundle")).isNotNull()
    }

    @Test
    fun `Variant specific extension overrides root`() {
        // language=gradle
        val config = """
            android {
                flavorDimensions 'pricing'
                productFlavors {
                    free { dimension 'pricing' }
                    paid { dimension 'pricing' }
                }

                playConfigs {
                    paidRelease {
                        track.set('variant')
                    }
                }
            }

            play {
                track = 'root'
            }
        """

        val result = execute(config, "help", "--debug")

        assertThat(result.output).contains("track=variant")
    }

    @Test
    fun `Flavor specific extension overrides root`() {
        // language=gradle
        val config = """
            android {
                flavorDimensions 'pricing'
                productFlavors {
                    free { dimension 'pricing' }
                    paid { dimension 'pricing' }
                }

                playConfigs {
                    paid {
                        track.set('flavor')
                    }
                }
            }

            play {
                track = 'root'
            }
        """

        val result = execute(config, "help", "--debug")

        assertThat(result.output).contains("track=flavor")
    }

    @Test
    fun `Build type specific extension overrides root`() {
        // language=gradle
        val config = """
            android {
                flavorDimensions 'pricing'
                productFlavors {
                    free { dimension 'pricing' }
                    paid { dimension 'pricing' }
                }

                playConfigs {
                    release {
                        track.set('build type')
                    }
                }
            }

            play {
                track = 'root'
            }
        """

        val result = execute(config, "help", "--debug")

        assertThat(result.output).contains("track=build type")
    }

    @Test
    fun `Root extension is used if no overrides are present`() {
        // language=gradle
        val config = """
            android.flavorDimensions 'pricing'
            android.productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }

            play {
                track = 'root'
            }
        """

        val result = execute(config, "help", "--debug")

        assertThat(result.output).contains("track=root")
    }

    @Test
    fun `Combination of extensions merges`() {
        // language=gradle
        val config = """
            android {
                flavorDimensions 'pricing'
                productFlavors {
                    free { dimension 'pricing' }
                    paid { dimension 'pricing' }
                }

                playConfigs {
                    paidRelease {
                        track.set('variant')
                    }

                    paid {
                        fromTrack.set('flavor')
                        defaultToAppBundles.set(true)
                    }

                    release {
                        fromTrack.set('build type')
                        promoteTrack.set('build type')
                    }
                }
            }

            play {
                track = 'root'
                releaseName = 'hello'
            }
        """

        val result = execute(config, "help", "--debug")

        assertThat(result.output).contains("defaultToAppBundles=true")
        assertThat(result.output).contains("track=variant")
        assertThat(result.output).contains("fromTrack=flavor")
        assertThat(result.output).contains("promoteTrack=build type")
        assertThat(result.output).contains("releaseName=hello")
    }

    @Test
    fun `No warnings are logged on valid playConfigs`() {
        // language=gradle
        val config = """
            android {
                flavorDimensions 'pricing'
                productFlavors {
                    free { dimension 'pricing' }
                    paid { dimension 'pricing' }
                }

                playConfigs {
                    paidRelease {}
                    paid {}
                    freeRelease {}
                    free {}
                    release {}
                }
            }

            play {
                track = 'root'
            }
        """

        val result = execute(config, "help")

        assertThat(result.output).doesNotContain("does not match")
    }

    @Test
    fun `Warning is logged on invalid playConfigs`() {
        // language=gradle
        val config = """
            android {
                flavorDimensions 'pricing'
                productFlavors {
                    free { dimension 'pricing' }
                    paid { dimension 'pricing' }
                }

                playConfigs {
                    foo {}
                }
            }
        """

        val result = execute(config, "help")

        assertThat(result.output).contains("does not match")
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun insertEdit(): EditResponse {
                    println("insertEdit()")
                    return newSuccessEditResponse("edit-id")
                }
            }
            publisher.install()
        }
    }
}
