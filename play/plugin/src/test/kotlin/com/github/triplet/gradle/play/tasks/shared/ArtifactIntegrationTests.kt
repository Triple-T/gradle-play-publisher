package com.github.triplet.gradle.play.tasks.shared

import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

interface ArtifactIntegrationTests : SharedIntegrationTest {
    @Test
    fun `Rebuilding artifact on-the-fly uses cached build`() {
        val result1 = execute("", taskName())
        val result2 = execute("", taskName())

        result1.requireTask(outcome = SUCCESS)
        result2.requireTask(outcome = UP_TO_DATE)
    }

    fun customArtifactName(): String

    fun assertCustomArtifactResults(result: BuildResult)

    @Test
    fun `Using custom artifact skips on-the-fly build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, customArtifactName()).safeCreateNewFile()
        val result = execute(config, taskName())

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains(customArtifactName())
        assertCustomArtifactResults(result)
    }

    @Test
    fun `Using custom artifact file skips on-the-fly build`() {
        val app = File(playgroundDir, customArtifactName()).safeCreateNewFile()
        // language=gradle
        val config = """
            play {
                artifactDir = file('${app.escaped()}')
            }
        """

        val result = execute(config, taskName())

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains(customArtifactName())
        assertCustomArtifactResults(result)
    }

    @ParameterizedTest
    @CsvSource(value = ["false,false", "false,true", "true,false", "true,true"])
    fun `Using custom artifact file with supported 3P dep skips on-the-fly build`(
            eager: Boolean,
            cliParam: Boolean
    ) {
        val app = File(playgroundDir, customArtifactName()).safeCreateNewFile()
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
            }

            play {
                serviceAccountCredentials = file('creds.json')
                ${if (cliParam) "" else "artifactDir = file('${app.escaped()}')"}
            }

            ${if (eager) "tasks.all {}" else ""}

            $factoryInstallerStatement
        """)

        val result = executeGradle(expectFailure = false) {
            withArguments(taskName())
            if (cliParam) {
                withArguments(arguments + listOf("--artifact-dir=${app}"))
            }
        }

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains(customArtifactName())
        assertCustomArtifactResults(result)
    }

    @Test
    fun `Reusing custom artifact uses cached build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, customArtifactName()).safeCreateNewFile()
        val result1 = execute(config, taskName())
        val result2 = execute(config, taskName())

        result1.requireTask(outcome = SUCCESS)
        result2.requireTask(outcome = UP_TO_DATE)
    }

    @Test
    fun `Using custom artifact correctly tracks dependencies`() {
        // language=gradle
        val config = """
            abstract class CustomTask extends DefaultTask {
                @OutputDirectory
                abstract DirectoryProperty getAppDir()

                @TaskAction
                void doStuff() {
                    appDir.get().file("${customArtifactName()}").asFile.createNewFile()
                }
            }

            def c = tasks.register("myCustomTask", CustomTask) {
                appDir.set(layout.projectDirectory.dir('${playgroundDir.escaped()}'))
            }

            play {
                artifactDir = c.flatMap { it.appDir }
            }
        """

        val result = execute(config, taskName())

        result.requireTask(":myCustomTask", outcome = SUCCESS)
        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains(customArtifactName())
        assertCustomArtifactResults(result)
    }

    @Test
    fun `Using custom artifact CLI arg skips on-the-fly build`() {
        File(playgroundDir, customArtifactName()).safeCreateNewFile()
        val result = execute("", taskName(), "--artifact-dir=${playgroundDir}")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains(customArtifactName())
        assertCustomArtifactResults(result)
    }

    @Test
    fun `Eagerly evaluated global CLI artifact-dir param skips on-the-fly build`() {
        // language=gradle
        val config = """
            playConfigs {
                release {
                    track.set('hello')
                }
            }

            tasks.all {}
        """.withAndroidBlock()

        File(playgroundDir, customArtifactName()).safeCreateNewFile()
        val result = execute(
                config,
                taskName(/* No variant: */ ""),
                "--artifact-dir=$playgroundDir"
        )

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains(playgroundDir.name)
        assertCustomArtifactResults(result)
    }
}
