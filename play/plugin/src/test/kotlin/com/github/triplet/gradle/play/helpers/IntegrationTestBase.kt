package com.github.triplet.gradle.play.helpers

import com.github.triplet.gradle.common.utils.orNull
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class IntegrationTestBase {
    @get:Rule
    val tempDir = TemporaryFolder()
    protected val appDir by lazy { File(tempDir.root, "app") }

    @Before
    fun initTestResources() {
        File("src/test/fixtures/app").copyRecursively(appDir)
        File("src/test/fixtures/${javaClass.simpleName}").orNull()?.copyRecursively(appDir)
    }

    protected fun escapedTempDir() = tempDir.root.toString().replace("\\", "\\\\")

    protected fun execute(config: String, vararg tasks: String) = execute(config, false, *tasks)

    protected fun executeExpectingFailure(config: String, vararg tasks: String) =
            execute(config, true, *tasks)

    protected fun executeGradle(
            expectFailure: Boolean,
            block: GradleRunner.() -> GradleRunner
    ): BuildResult {
        val runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(appDir)
                .let(block)

        // We're doing some pretty wack (and disgusting, shameful) shit to run integration tests without
        // actually publishing anything. The idea is have the build file call into the test class to run
        // some code. Unfortunately, it'll mostly be limited to printlns since we can't actually share
        // any state due to the same code being run in completely different classpaths (possibly
        // even different processes), but at least we can validate that tasks are trying to publish the
        // correct stuff now.
        runner.withPluginClasspath(runner.pluginClasspath + listOf(
                File("build/classes/kotlin/test"),
                File("../android-publisher/build/resources/testFixtures")
        ))

        val result = if (expectFailure) runner.buildAndFail() else runner.build()
        println(result.output)
        return result
    }

    private fun execute(
            config: String,
            expectFailure: Boolean,
            vararg tasks: String
    ): BuildResult {
        val buildCacheDir = File(tempDir.root, "gradle").path.replace("\\", "\\\\")

        // language=gradle
        File(appDir, "settings.gradle").writeText("""
            buildCache.local.directory = new File('$buildCacheDir')
        """)

        // language=gradle
        File(appDir, "build.gradle").writeText("""
            plugins {
                id 'com.android.application'
                id 'com.github.triplet.play'
            }

            allprojects {
                repositories {
                    google()
                    jcenter()
                }
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

                $config
            }

            play {
                serviceAccountCredentials = file('creds.json')
            }
        """)

        return executeGradle(expectFailure) {
            withArguments("-S", "--build-cache", *tasks)
        }
    }
}
