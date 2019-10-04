package com.github.triplet.gradle.play.helpers

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

val FIXTURE_WORKING_DIR = File("src/test/fixtures/android_app")
val BUILD_FILE = File(FIXTURE_WORKING_DIR, "build.gradle")

fun execute(config: String, vararg tasks: String) = execute(config, false, *tasks)

fun executeExpectingFailure(config: String, vararg tasks: String) = execute(config, true, *tasks)

private fun execute(
        config: String,
        expectFailure: Boolean,
        vararg tasks: String
): BuildResult {
    // language=gradle
    BUILD_FILE.writeText("""
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
                applicationId "com.github.triplet.gradle.play.test"
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

    val runner = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(FIXTURE_WORKING_DIR)
            .withArguments("-S", *tasks)

    // We're doing some pretty wack (and disgusting, shameful) shit to run integration tests without
    // actually publishing anything. The idea is have the build file call into the test class to run
    // some code. Unfortunately, it'll mostly be limited to printlns since we can't actually share
    // any state due to the same code being run in completely different classpaths (possibly
    // even different processes), but at least we can validate that tasks are trying to publish the
    // correct stuff now.
    runner.withPluginClasspath(runner.pluginClasspath + listOf(
            File("build/classes/kotlin/test")
    ))

    return try {
        if (expectFailure) runner.buildAndFail() else runner.build()
    } finally {
        BUILD_FILE.delete()
    }
}
