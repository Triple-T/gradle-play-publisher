package com.github.triplet.gradle.play

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.net.URI

@RunWith(Parameterized::class)
class CompatibilityTest(
        private val agpVersion: String,
        private val gradleVersion: String
) {
    private val pluginBinaryDir = File("build/libs")
    private val pluginVersionName = System.getProperty("VERSION_NAME")

    private lateinit var testProject: Project

    @Before
    fun setup() {
        testProject = ProjectBuilder.builder()
                .withProjectDir(File("src/test/fixtures/android_app"))
                .build()
    }

    @Test
    fun pluginIsCompatible() {
        assert(pluginTest())
    }

    private fun pluginTest(): Boolean {
        val pluginJar = pluginBinaryDir
                .listFiles()
                .first { it.name.endsWith("$pluginVersionName.jar") }
                .absoluteFile
                .invariantSeparatorsPath

        // language=gradle
        File(testProject.projectDir, "build.gradle").writeText("""
        buildscript {
            repositories {
                google()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:$agpVersion'
                classpath files("$pluginJar")

                // Manually define transitive dependencies for our plugin since we don't have the
                // POM to fetch them for us
                classpath('com.google.apis:google-api-services-androidpublisher:v3-rev12-1.23.0') {
                    exclude group: 'com.google.guava', module: 'guava-jdk5'
                }
            }
        }

        apply plugin: 'com.android.application'
        apply plugin: 'com.github.triplet.play'

        android {
            compileSdkVersion 27
            buildToolsVersion '27.0.3'
            lintOptions {
                abortOnError false
            }

            defaultConfig {
                applicationId "com.github.triplet.gradle.play.test"
                minSdkVersion 21
                targetSdkVersion 27
                versionCode 1
                versionName "1.0"
            }
        }

        play {
            serviceAccountCredentials = file('some-file.json')
        }
        """)

        val gradleDist = "https://services.gradle.org/distributions/gradle-$gradleVersion-all.zip"

        GradleRunner.create()
                .withPluginClasspath()
                .withGradleDistribution(URI(gradleDist))
                .withProjectDir(testProject.projectDir)
                .withArguments("tasks")
                .build()
        GradleRunner.create()
                .withPluginClasspath()
                .withGradleDistribution(URI(gradleDist))
                .withProjectDir(testProject.projectDir)
                .withArguments("clean")
                .build()

        return true
    }

    @After
    fun cleanup() {
        File(testProject.projectDir, "build.gradle").delete()
    }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "agpVersion: {0}, gradleVersion {1}")
        fun parameters() = listOf(
                arrayOf("3.0.1", "4.1"), // Oldest supported
                arrayOf("3.2.0-rc02", "4.6"), // Latest stable
                arrayOf("3.3.0-alpha07", "4.10") // Latest
        )
    }
}
