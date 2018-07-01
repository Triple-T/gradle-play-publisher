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
        val agpVersion: String,
        val gradleVersion: String
) {
    private lateinit var testProject: Project
    private lateinit var pluginProjectDir: File

    @Before
    fun setup() {
        pluginProjectDir = File(".")
        testProject = ProjectBuilder.builder()
                .withProjectDir(File("src/test/fixtures/android_app"))
                .build()
    }

    @Test
    fun pluginIsCompatible() {
        assert(pluginTest())
    }

    private fun pluginTest(): Boolean {
        val pluginJar = File(pluginProjectDir, "/build/libs")
                .listFiles()
                .first {
                    // This is ugly, we just want the proper jar from the build folder
                    it.name.endsWith("jar") &&
                            !it.name.contains("javadoc", true) &&
                            !it.name.contains("sources", true)
                }
                .absolutePath

        File(testProject.projectDir, "build.gradle").writeText("""
        buildscript {
            repositories {
                google()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:$agpVersion'
                classpath files("$pluginJar")

                // manually defining transitive dependencies for our plugin
                // as we don't have the pom but only the compiled jar
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
                arrayOf("3.0.1", "4.4"),
                arrayOf("3.0.1", "4.6"),
                arrayOf("3.0.1", "4.8"),
                arrayOf("3.1.3", "4.4"),
                arrayOf("3.1.3", "4.6"),
                arrayOf("3.1.3", "4.8"),
                arrayOf("3.2.0-beta02", "4.4"),
                arrayOf("3.2.0-beta02", "4.6"),
                arrayOf("3.2.0-beta02", "4.8")
        )
    }
}
