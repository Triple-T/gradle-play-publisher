package com.github.triplet.gradle.play

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

val FIXTURE_WORKING_DIR = File("src/test/fixtures/android_app")
val BUILD_FILE = File(FIXTURE_WORKING_DIR, "build.gradle")

fun execute(config: String, vararg tasks: String) = execute(config, false, null, null, *tasks)

fun executeExpectingFailure(config: String, vararg tasks: String) =
        execute(config, true, null, null, *tasks)

fun executeWithVersions(
        config: String,
        gradleVersion: String,
        agpVersion: String,
        vararg tasks: String
) = execute(config, false, gradleVersion, agpVersion, *tasks)

private fun execute(
        config: String,
        expectFailure: Boolean,
        gradleVersion: String?,
        agpVersion: String?,
        vararg tasks: String
): BuildResult {
    // language=gradle
    BUILD_FILE.writeText("""
        buildscript {
            repositories {
                google()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:${agpVersion ?: "3.5.0"}'
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
                applicationId "com.github.triplet.gradle.play.test"
                minSdkVersion 21
                targetSdkVersion 28
                versionCode 1
                versionName "1.0"
            }

            $config
        }

        play {
            serviceAccountCredentials = file('some-file.json')
        }
    """)

    val runner = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(FIXTURE_WORKING_DIR)
            .withArguments(*tasks)
    if (gradleVersion != null) runner.withGradleVersion(gradleVersion)

    return try {
        if (expectFailure) runner.buildAndFail() else runner.build()
    } finally {
        BUILD_FILE.delete()
    }
}
