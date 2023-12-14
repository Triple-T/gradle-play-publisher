package com.github.triplet.gradle.play.tasks.shared

import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest.Companion.DEFAULT_TASK_VARIANT
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

interface ArtifactIntegrationTests : SharedIntegrationTest {
    fun customArtifactName(name: String = "foo"): String

    fun assertCustomArtifactResults(result: BuildResult, executed: Boolean = true)

    @Test
    fun `Rebuilding artifact on-the-fly uses cached build`() {
        val result1 = execute("", taskName())
        val result2 = execute("", taskName())

        result1.requireTask(outcome = SUCCESS)
        result2.requireTask(outcome = UP_TO_DATE)
    }

    @Test
    fun `Using non-existent custom artifact skips build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        val result = execute(config, taskName())

        assertCustomArtifactResults(result, executed = false)
        result.requireTask(outcome = NO_SOURCE)
    }

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
    @CsvSource(value = [
        "false,false,",
        "false,true,",
        "true,false,",
        "true,true,",
        "false,false,$DEFAULT_TASK_VARIANT",
        "false,true,$DEFAULT_TASK_VARIANT",
        "true,false,$DEFAULT_TASK_VARIANT",
        "true,true,$DEFAULT_TASK_VARIANT"
    ])
    fun `Using custom artifact file with supported 3P dep skips on-the-fly build`(
            eager: Boolean,
            cliParam: Boolean,
            taskVariant: String?,
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
                compileSdk 34
                namespace = "com.example.publisher"

                defaultConfig {
                    applicationId "com.supercilex.test"
                    minSdk 31
                    targetSdk 33
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
            withArguments(taskName(taskVariant.orEmpty()))
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

    @ParameterizedTest
    @ValueSource(strings = ["", DEFAULT_TASK_VARIANT])
    fun `Using custom artifact CLI arg skips on-the-fly build`(taskVariant: String) {
        File(playgroundDir, customArtifactName()).safeCreateNewFile()
        val result = execute("", taskName(taskVariant), "--artifact-dir=${playgroundDir}")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains(customArtifactName())
        assertCustomArtifactResults(result)
    }

    @Test
    fun `Eagerly evaluated global CLI artifact-dir param skips on-the-fly build`() {
        // language=gradle
        val config = """
            tasks.all {}
        """

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

    @Test
    fun `Using custom artifact with multiple files uploads each one`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, customArtifactName("1")).safeCreateNewFile()
        File(playgroundDir, customArtifactName("2")).safeCreateNewFile()
        val result = execute(config, taskName())

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains(customArtifactName("1"))
        assertThat(result.output).contains(customArtifactName("2"))
    }
}
