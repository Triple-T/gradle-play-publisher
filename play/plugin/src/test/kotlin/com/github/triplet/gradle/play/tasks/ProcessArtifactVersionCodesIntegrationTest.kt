package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import java.io.File

class ProcessArtifactVersionCodesIntegrationTest : IntegrationTestBase(), SharedIntegrationTest {
    override fun taskName(taskVariant: String) = ":process${taskVariant}VersionCodes"

    @Test
    fun `Task only runs on release`() {
        // language=gradle
        val config = """
            play.resolutionStrategy = ResolutionStrategy.AUTO
        """

        val result = execute(config, "assembleDebug")

        assertThat(result.task(":processDebugMetadata")).isNull()
    }

    @Test
    fun `Task doesn't run by default`() {
        val result = execute("", "assembleRelease")

        assertThat(result.task(":processReleaseVersionCodes")).isNull()
    }

    @Test
    fun `Version code is incremented`() {
        // language=gradle
        val config = """
            play.resolutionStrategy = ResolutionStrategy.AUTO

            androidComponents {
                onVariants(selector().all()) {
                    for (output in outputs) {
                        output.versionName.set(output.versionCode.map {
                            println('versionCode=' + it)
                            it.toString()
                        })
                    }
                }
            }
        """

        val result = execute(config, "assembleRelease")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("versionCode=42")
    }

    @Test
    fun `Version code isn't modified when no changes are necessary`() {
        // language=gradle
        val config = """
            play.resolutionStrategy = ResolutionStrategy.AUTO
            android.defaultConfig.versionCode = 42

            androidComponents {
                onVariants(selector().all()) {
                    for (output in outputs) {
                        output.versionName.set(output.versionCode.map {
                            println('versionCode=' + it)
                            it.toString()
                        })
                    }
                }
            }
        """

        val result = execute(config, "assembleRelease")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("versionCode=42")
    }

    @Test
    fun `Version code with splits is patch incremented`() {
        // language=gradle
        File(appDir, "build.gradle").writeText("""
            plugins {
                id 'com.android.application'
                id 'com.github.triplet.play' apply false
            }

            allprojects {
                repositories {
                    google()
                    mavenCentral()
                }
            }

            android {
                compileSdk 31

                defaultConfig {
                    applicationId "com.example.publisher"
                    minSdk 31
                    targetSdk 31
                    versionCode 1
                    versionName "1.0"
                }
            }

            $factoryInstallerStatement

            android.splits.density {
                enable true
                reset()
                include "xxhdpi", "xxxhdpi"
            }

            def count = 0
            androidComponents {
                onVariants(selector().withBuildType('release')) {
                    for (output in outputs) {
                        output.versionCode.set(count++)
                        output.versionName.set(output.versionCode.map {
                            println('versionCode=' + it)
                            it.toString()
                        })
                    }
                }
            }

            apply plugin: 'com.github.triplet.play'
            play {
                serviceAccountCredentials = file('creds.json')
                resolutionStrategy =
                    com.github.triplet.gradle.androidpublisher.ResolutionStrategy.AUTO
            }
        """)

        val result = executeGradle(false) {
            withArguments("assembleRelease")
        }

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("versionCode=42")
        assertThat(result.output).contains("versionCode=44")
        assertThat(result.output).contains("versionCode=46")
    }

    @Test
    fun `Version code isn't eagerly evaluated in non-auto resolution`() {
        // language=gradle
        File(appDir, "build.gradle").writeText("""
            plugins {
                id 'com.android.application'
                id 'com.github.triplet.play' apply false
            }

            allprojects {
                repositories {
                    google()
                    mavenCentral()
                }
            }

            android {
                compileSdk 31

                defaultConfig {
                    applicationId "com.example.publisher"
                    minSdk 31
                    targetSdk 31
                    versionCode 1
                    versionName "1.0"
                }
            }

            $factoryInstallerStatement

            abstract class CustomTask extends DefaultTask {
                @OutputFile
                abstract RegularFileProperty getVersionCodeFile()

                @TaskAction
                void doStuff() {
                    versionCodeFile.get().asFile.text = '88'
                }
            }

            def c = tasks.register("myCustomTask", CustomTask) {
                versionCodeFile.set(layout.buildDirectory.file('blah/custom-version-code.txt'))
            }

            androidComponents {
                onVariants(selector().all()) {
                    for (output in outputs) {
                        output.versionCode.set(c.map {
                            it.versionCodeFile.get().asFile.text as Integer
                        })
                        output.versionName.set(output.versionCode.map {
                            println('versionCode=' + it)
                            it.toString()
                        })
                    }
                }
            }

            apply plugin: 'com.github.triplet.play'
        """)

        val result = executeGradle(false) {
            withArguments("assembleRelease")
        }

        assertThat(result.task(taskName())).isNull()
        result.requireTask(outcome = SUCCESS, task = ":myCustomTask")
        assertThat(result.output).contains("versionCode=88")
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun insertEdit(): EditResponse {
                    println("insertEdit()")
                    return newSuccessEditResponse("edit-id")
                }

                override fun getEdit(id: String): EditResponse {
                    println("getEdit($id)")
                    return newSuccessEditResponse(id)
                }
            }
            val edits = object : FakeEditManager() {
                override fun findMaxAppVersionCode(): Long = 41
            }

            publisher.install()
            edits.install()
        }
    }
}
