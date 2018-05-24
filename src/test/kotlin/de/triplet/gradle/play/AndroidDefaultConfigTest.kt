package de.triplet.gradle.play

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.test.*

@Ignore("Should only run these as strictly necessary, they are high overhead")
class AndroidDefaultConfigTest {
    private lateinit var kotlinProject: Project
    private lateinit var groovyProject: Project
    private lateinit var testProject: Project

    private val GradleVersion = "4.6"

    private val gradleVersions = arrayOf("2.2.3", "2.3.0", "3.0.1", "3.1.0", "3.2.0-alpha15")

    @Before
    fun setup() {
        kotlinProject = ProjectBuilder.builder()
                .withProjectDir(File("src/test/fixtures/plugin_kotlin"))
                .build()
        groovyProject = ProjectBuilder.builder()
                .withProjectDir(File("src/test/fixtures/plugin_groovy"))
                .build()
        testProject = ProjectBuilder.builder()
                .withProjectDir(File("src/test/fixtures/android_app"))
                .build()
    }

    @After
    fun cleanup() {
        File(kotlinProject.projectDir, "build.gradle").delete()
        File(groovyProject.projectDir, "build.gradle").delete()
        File(testProject.projectDir, "build.gradle").delete()
    }

    @Test
    fun `Test Android plugin defaultConfig for Kotlin`() {
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, gradleVersions[0], gradleVersions[0])
        }
        cleanup()
        assert(
            pluginTest(kotlinProject, gradleVersions[0], gradleVersions[1])
        )
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, gradleVersions[0], gradleVersions[2])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, gradleVersions[0], gradleVersions[3])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, gradleVersions[0], gradleVersions[4])
        }
        cleanup()
        assert(
            pluginTest(kotlinProject, gradleVersions[1], gradleVersions[1])
        )
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, gradleVersions[1], gradleVersions[2])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, gradleVersions[1], gradleVersions[3])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, gradleVersions[1], gradleVersions[4])
        }
        cleanup()
        assert(
            pluginTest(kotlinProject, gradleVersions[2], gradleVersions[2])
        )
        cleanup()
        assert(
            pluginTest(kotlinProject, gradleVersions[2], gradleVersions[3])
        )
        cleanup()
        assert(
            pluginTest(kotlinProject, gradleVersions[2], gradleVersions[4])
        )
        cleanup()
        assert(
            pluginTest(kotlinProject, gradleVersions[3], gradleVersions[3])
        )
        cleanup()
        assert(
            pluginTest(kotlinProject, gradleVersions[3], gradleVersions[4])
        )
        cleanup()
        assert(
            pluginTest(kotlinProject, gradleVersions[4], gradleVersions[4])
        )
        cleanup()
    }

    @Test
    fun `Test Android plugin defaultConfig for Groovy`() {
        //This fails because of an Android problem, not an incompatiblity like the Kotlin tests
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(groovyProject, gradleVersions[0], gradleVersions[0])
        }
        cleanup()
        assert(
                pluginTest(groovyProject, gradleVersions[0], gradleVersions[1])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[0], gradleVersions[2])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[0], gradleVersions[3])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[0], gradleVersions[4])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[1], gradleVersions[1])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[1], gradleVersions[2])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[1], gradleVersions[3])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[1], gradleVersions[4])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[2], gradleVersions[2])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[2], gradleVersions[3])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[2], gradleVersions[4])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[3], gradleVersions[3])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[3], gradleVersions[4])
        )
        cleanup()
        assert(
            pluginTest(groovyProject, gradleVersions[4], gradleVersions[4])
        )
        cleanup()
    }

    private fun pluginTest(pluginProject: Project, pluginVersion: String, appVersion: String): Boolean {
        File(pluginProject.projectDir, "build.gradle").writeText("""
        |plugins {
        |    id 'groovy'
        |    id 'org.jetbrains.kotlin.jvm' version '1.2.41'
        |    id 'java-gradle-plugin'
        |}
        |
        |sourceCompatibility = JavaVersion.VERSION_1_7
        |targetCompatibility = JavaVersion.VERSION_1_7
        |
        |repositories {
        |    jcenter()
        |    google()
        |}
        |
        |group='com.github.triplet.play.test'
        |version='1'
        |
        |dependencies {
        |    implementation 'com.android.tools.build:gradle:$pluginVersion'
        |    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.2.41'
        |}
        |""".trimMargin())

        GradleRunner.create()
                .withPluginClasspath()
                .withGradleVersion(GradleVersion)
                .withProjectDir(pluginProject.projectDir)
                .withArguments("assemble")
                .build()

        val pluginJar = File(pluginProject.buildDir, "/libs").listFiles().first { it.name.endsWith("jar") }.absolutePath

        File(testProject.projectDir, "build.gradle").writeText("""
        |buildscript {
        |    repositories {
        |        google()
        |        jcenter()
        |    }
        |    dependencies {
        |        classpath 'com.android.tools.build:gradle:$appVersion'
        |        classpath files("$pluginJar")
        |    }
        |}
        |
        |apply plugin: 'com.android.application'
        |apply plugin: 'com.github.triplet.play.test'
        |
        |android {
        |    compileSdkVersion 27
        |    buildToolsVersion '27.0.3'
        |    lintOptions {
        |        abortOnError false
        |    }
        |
        |    defaultConfig {
        |        applicationId "de.triplet.gradle.play.test"
        |        minSdkVersion 21
        |        targetSdkVersion 27
        |        versionCode 1
        |        versionName "1.0"
        |    }
        |}
        |
        |""".trimMargin())

        GradleRunner.create()
                .withPluginClasspath()
                .withGradleVersion(GradleVersion)
                .withProjectDir(testProject.projectDir)
                .withArguments("tasks")
                .build()

        GradleRunner.create()
                .withPluginClasspath()
                .withGradleVersion(GradleVersion)
                .withProjectDir(testProject.projectDir)
                .withArguments("clean")
                .build()
        GradleRunner.create()
                .withPluginClasspath()
                .withGradleVersion(GradleVersion)
                .withProjectDir(pluginProject.projectDir)
                .withArguments("clean")
                .build()
        return true
    }
}