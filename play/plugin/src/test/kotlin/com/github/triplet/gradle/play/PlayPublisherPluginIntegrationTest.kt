package com.github.triplet.gradle.play

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
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
    fun `Debuggable build types are ignored`() {
        // language=gradle
        val config = """
            buildTypes {
                release {
                    debuggable true
                }
            }
        """

        val result = execute(config, "tasks", "--group", "publishing")

        assertThat(result.output).doesNotContain("publishRelease")
    }

    @Test
    fun `Disabled build types are ignored`() {
        // language=gradle
        val config = """
            android.variantFilter { variant ->
                variant.setIgnore(true)
            }
        """

        val result = execute(config, "tasks", "--group", "publishing")

        assertThat(result.output).doesNotContain("publishRelease")
    }

    @Test
    fun `Disabled GPP variant is ignored`() {
        // language=gradle
        val config = """
            android.playConfigs {
                release {
                    setEnabled false
                }
            }
        """

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
    fun `PKCS creds logs warning`() {
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

            play {
                serviceAccountCredentials = file('name')
                serviceAccountEmail = 'email'
            }
        """)

        val result = executeGradle(false) {
            withArguments("help", "--warning-mode", "all")
        }

        assertThat(result.output).contains("PKCS12 based authentication is deprecated")
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
                defaultToAppBundles true
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
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }

            play {
                track 'root'
            }

            playConfigs {
                paidRelease {
                    track = 'variant'
                }
            }
        """

        val result = execute(config, "help", "--debug")

        assertThat(result.output).contains("track=variant")
    }

    @Test
    fun `Flavor specific extension overrides root`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }

            play {
                track 'root'
            }

            playConfigs {
                paid {
                    track = 'flavor'
                }
            }
        """

        val result = execute(config, "help", "--debug")

        assertThat(result.output).contains("track=flavor")
    }

    @Test
    fun `Build type specific extension overrides root`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }

            play {
                track 'root'
            }

            playConfigs {
                release {
                    track = 'build type'
                }
            }
        """

        val result = execute(config, "help", "--debug")

        assertThat(result.output).contains("track=build type")
    }

    @Test
    fun `Root extension is used if no overrides are present`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }

            play {
                track 'root'
            }
        """

        val result = execute(config, "help", "--debug")

        assertThat(result.output).contains("track=root")
    }

    @Test
    fun `Combination of extensions merges`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }

            play {
                track 'root'
                releaseName 'hello'
            }

            playConfigs {
                paidRelease {
                    track = 'variant'
                }

                paid {
                    fromTrack = 'flavor'
                    defaultToAppBundles = true
                }

                release {
                    fromTrack = 'build type'
                    promoteTrack = 'build type'
                }
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
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }

            play {
                track 'root'
            }

            playConfigs {
                paidRelease {}
                paid {}
                freeRelease {}
                free {}
                release {}
            }
        """

        val result = execute(config, "help")

        assertThat(result.output).doesNotContain("does not match")
    }

    @Test
    fun `Warning is logged on invalid playConfigs`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }

            playConfigs {
                foo {}
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
