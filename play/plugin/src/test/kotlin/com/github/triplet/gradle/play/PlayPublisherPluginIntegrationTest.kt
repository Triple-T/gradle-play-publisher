package com.github.triplet.gradle.play

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

class PlayPublisherPluginIntegrationTest : IntegrationTestBase() {
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

    @ParameterizedTest
    @CsvSource(value = [
        "false,false",
        "false,true",
        "true,false",
        "true,true"
    ])
    fun `Credentials are validated and used`(envvar: Boolean, present: Boolean) {
        // language=gradle
        File(appDir, "build.gradle").writeText("""
            import com.github.triplet.gradle.play.tasks.internal.PlayApiService

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

            task usePublisher {
                doLast {
                    def service = gradle.sharedServices.registrations
                                .named("playApi-com.example.publisher")
                                .get().service.get() as PlayApiService

                    service.publisher
                }
            }

            if (${present && !envvar}) {
                play.serviceAccountCredentials.set(file('creds.json'))
            }

            $factoryInstallerStatement
        """)

        executeGradle(!present) {
            withArguments("usePublisher")
            if (present && envvar) {
                withEnvironment(mapOf(PlayPublisher.CREDENTIAL_ENV_VAR to "fake-creds"))
            }
        }
    }

    @Test
    fun `Variant specific lifecycle task publishes APKs by default`() {
        val result = execute("", "publishReleaseApps", "--dry-run")

        assertThat(result.output).contains(":publishReleaseApk")
        assertThat(result.output).doesNotContain(":publishReleaseBundle")
    }

    @Test
    fun `Variant specific lifecycle task publishes App Bundle when specified`() {
        // language=gradle
        val config = """
            play {
                defaultToAppBundles = true
            }
        """

        val result = execute(config, "publishReleaseApps", "--dry-run")

        assertThat(result.output).doesNotContain(":publishReleaseApk")
        assertThat(result.output).contains(":publishReleaseBundle")
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
    fun `Dimension specific extension overrides root`() {
        // language=gradle
        val config = """
            android {
                flavorDimensions 'pricing'
                productFlavors {
                    free { dimension 'pricing' }
                    paid { dimension 'pricing' }
                }

                playConfigs {
                    pricing {
                        track.set('dimension')
                    }
                }
            }

            play {
                track = 'root'
            }
        """

        val result = execute(config, "help", "--debug")

        assertThat(result.output).contains("track=dimension")
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
    fun `Play API service uses correct credentials`() {
        // language=gradle
        val config = """
            import com.github.triplet.gradle.play.tasks.internal.PlayApiService

            android.flavorDimensions 'a', 'b'
            android.productFlavors {
                special {
                    dimension 'a'
                    applicationId = 'special'
                }
                general { dimension 'b' }
            }
            android.buildTypes {
                zzz { // Add another build alphabetically below release so we can't cheat
                    initWith(release)
                }
            }

            android.playConfigs {
                specialGeneralRelease {
                    serviceAccountCredentials.set(file('special'))
                }

                general {
                    serviceAccountCredentials.set(file('general'))
                }
            }

            task usePublisher {
                doLast {
                    def service = gradle.sharedServices.registrations
                                .named('playApi-special')
                                .get().service.get() as PlayApiService

                    println('creds=' + service.parameters.credentials.get().asFile.name)
                }
            }
        """

        val result = execute(config, "usePublisher")

        assertThat(result.output).contains("creds=special")
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
                        fromTrack.set('paid flavor')
                        defaultToAppBundles.set(true)
                    }

                    pricing {
                        fromTrack.set('pricing dimension')
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
        assertThat(result.output).contains("fromTrack=paid flavor")
        assertThat(result.output).contains("fromTrack=pricing dimension")
        assertThat(result.output).contains("promoteTrack=build type")
        assertThat(result.output).contains("releaseName=hello")
    }

    @Test
    fun `Semi-global task CLI args changes extension`() {
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

            task printTrack {
                doLast {
                    println 'resolvedTrack=' + play.track.get()
                }
            }

            afterEvaluate {
                tasks.named('publishFreeReleaseApk') {
                    enabled = false
                    dependsOn('printTrack')
                }
            }
        """

        val result = execute(
                config,
                "publishFreeApk", "--track=free",
                "--debug",
        )

        assertThat(result.output).contains("resolvedTrack=free")
    }

    @Test
    fun `Same-level extensions are resolved independently`() {
        // language=gradle
        val config = """
            android {
                flavorDimensions 'pricing'
                productFlavors {
                    free { dimension 'pricing' }
                    paid { dimension 'pricing' }
                }

                playConfigs {
                    free {
                        serviceAccountCredentials.set(file('free-creds.json'))
                    }

                    paid {
                        serviceAccountCredentials.set(file('paid-creds.json'))
                    }
                }
            }
        """

        val result = execute(config, "help", "--debug")
        val resolvedLines = result.output.lines()
                .filter { "Extension resolved for variant" in it }
                .filter { "Release:" in it }

        assertThat(resolvedLines).hasSize(2)
        assertThat(resolvedLines.first()).contains("free-creds.json")
        assertThat(resolvedLines.last()).contains("paid-creds.json")
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
                    pricing {}
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

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `Crashlytics runs on publish`(flavors: Boolean) {
        // language=gradle
        val flavorsConfig = """
            flavorDimensions "version"
            productFlavors {
                demo {
                    dimension "version"
                    applicationIdSuffix ".demo"
                    versionNameSuffix "-demo"
                }

                full {
                    dimension "version"
                    applicationIdSuffix ".full"
                    versionNameSuffix "-full"
                }
            }
        """
        // language=gradle
        File(appDir, "build.gradle").writeText("""
            buildscript {
                repositories.google()

                dependencies.classpath 'com.google.firebase:firebase-crashlytics-gradle:2.4.1'
            }

            plugins {
                id 'com.android.application'
                id 'com.github.triplet.play'
            }
            apply plugin: 'com.google.firebase.crashlytics'

            android {
                compileSdkVersion 28

                defaultConfig {
                    applicationId "com.supercilex.test"
                    minSdkVersion 21
                    targetSdkVersion 28
                    versionCode 1
                    versionName "1.0"
                }

                buildTypes.release {
                    shrinkResources true
                    minifyEnabled true
                    proguardFiles(getDefaultProguardFile("proguard-android.txt"))
                }

                ${flavorsConfig.takeIf { flavors } ?: ""}
            }

            play {
                serviceAccountCredentials = file('creds.json')
            }
        """)

        val flavor = if (flavors) "FullRelease" else "Release"
        val crashingSensitivePublishingTasks = setOf(
                "publishApk",
                "publishBundle",
                "upload${flavor}PrivateApk",
                "upload${flavor}PrivateBundle"
        )
        for (task in crashingSensitivePublishingTasks) {
            val result = executeGradle(false) {
                withArguments(task, "--dry-run")
            }

            assertThat(result.output).contains(":uploadCrashlyticsMappingFile$flavor")
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `Bugsnag runs on publish`(flavors: Boolean) {
        val classpathJars = GradleRunner.create().withPluginClasspath().pluginClasspath
                .joinToString { "'$it'" }

        // language=gradle
        val flavorsConfig = """
            flavorDimensions "version"
            productFlavors {
                demo {
                    dimension "version"
                    applicationIdSuffix ".demo"
                    versionNameSuffix "-demo"
                }

                full {
                    dimension "version"
                    applicationIdSuffix ".full"
                    versionNameSuffix "-full"
                }
            }
        """
        // language=gradle
        File(appDir, "build.gradle").writeText("""
           buildscript {
                repositories.mavenCentral()

                dependencies.classpath files($classpathJars)
                dependencies.classpath 'com.bugsnag:bugsnag-android-gradle-plugin:7.0.0-beta01'
            }

            apply plugin: 'com.android.application'
            apply plugin: 'com.github.triplet.play'
            apply plugin: 'com.bugsnag.android.gradle'

            android {
                compileSdkVersion 28

                defaultConfig {
                    applicationId "com.supercilex.test"
                    minSdkVersion 21
                    targetSdkVersion 28
                    versionCode 1
                    versionName "1.0"
                }

                buildTypes.release {
                    shrinkResources true
                    minifyEnabled true
                    proguardFiles(getDefaultProguardFile("proguard-android.txt"))
                }

                ${flavorsConfig.takeIf { flavors } ?: ""}
            }

            play {
                serviceAccountCredentials = file('creds.json')
            }
        """)

        val flavor = if (flavors) "FullRelease" else "Release"
        val crashingSensitivePublishingTasks = setOf(
                "publishApk",
                "publishBundle",
                "upload${flavor}PrivateApk",
                "upload${flavor}PrivateBundle"
        )
        for (task in crashingSensitivePublishingTasks) {
            val result = executeGradle(false) {
                withArguments(task, "--dry-run")
            }

            assertThat(result.output).contains(":uploadBugsnag")
        }
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
