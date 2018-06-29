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
class CompatibilityTest(val agpVersion: String) {
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
        assert(pluginTest(agpVersion))
    }

    private fun pluginTest(appAgpVersion: String): Boolean {
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
                classpath 'com.android.tools.build:gradle:$appAgpVersion'
                classpath files("$pluginJar")
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
        """)

        GradleRunner.create()
                .withPluginClasspath()
                .withGradleDistribution(URI(GRADLE_DIST))
                .withProjectDir(testProject.projectDir)
                .withArguments("tasks")
                .build()
        GradleRunner.create()
                .withPluginClasspath()
                .withGradleDistribution(URI(GRADLE_DIST))
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
        const val GRADLE_DIST = "https://services.gradle.org/distributions/gradle-4.6-all.zip"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun agpVersions() = listOf("3.0.1", "3.1.3", "3.2.0-beta02")
    }
}
