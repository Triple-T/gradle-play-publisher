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
import java.net.URI
import kotlin.test.assertFailsWith

@Ignore("Run only when necessary to avoid high overhead")
class AndroidDefaultConfigTest {
    private lateinit var kotlinProject: Project
    private lateinit var groovyProject: Project
    private lateinit var testProject: Project

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

    @Test
    fun `Test Android plugin defaultConfig for Kotlin`() {
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[0], agpVersions[0])
        }
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[0], agpVersions[1])
        )
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[0], agpVersions[2])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[0], agpVersions[3])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[0], agpVersions[4])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[1], agpVersions[0])
        }
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[1], agpVersions[1])
        )
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[1], agpVersions[2])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[1], agpVersions[3])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[1], agpVersions[4])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[2], agpVersions[0])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[2], agpVersions[1])
        }
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[2], agpVersions[2])
        )
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[2], agpVersions[3])
        )
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[2], agpVersions[4])
        )
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[3], agpVersions[0])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[3], agpVersions[1])
        }
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[3], agpVersions[2])
        )
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[3], agpVersions[3])
        )
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[3], agpVersions[4])
        )
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[4], agpVersions[0])
        }
        cleanup()
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(kotlinProject, agpVersions[4], agpVersions[1])
        }
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[4], agpVersions[2])
        )
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[4], agpVersions[3])
        )
        cleanup()
        assert(
                pluginTest(kotlinProject, agpVersions[4], agpVersions[4])
        )
        cleanup()
    }

    @Test
    fun `Test Android plugin defaultConfig for Groovy`() {
        //This fails because of an Android problem, not an incompatiblity like the Kotlin tests
        assertFailsWith(UnexpectedBuildFailure::class) {
            pluginTest(groovyProject, agpVersions[0], agpVersions[0])
        }
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[0], agpVersions[1])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[0], agpVersions[2])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[0], agpVersions[3])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[0], agpVersions[4])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[1], agpVersions[1])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[1], agpVersions[2])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[1], agpVersions[3])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[1], agpVersions[4])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[2], agpVersions[2])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[2], agpVersions[3])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[2], agpVersions[4])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[3], agpVersions[3])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[3], agpVersions[4])
        )
        cleanup()
        assert(
                pluginTest(groovyProject, agpVersions[4], agpVersions[4])
        )
        cleanup()
    }

    private fun pluginTest(
            pluginProject: Project,
            pluginVersion: String,
            appVersion: String
    ): Boolean {
        File(pluginProject.projectDir, "build.gradle").writeText("""
        plugins {
            id 'groovy'
            id 'org.jetbrains.kotlin.jvm' version '1.2.41'
            id 'java-gradle-plugin'
        }

        sourceCompatibility = JavaVersion.VERSION_1_7
        targetCompatibility = JavaVersion.VERSION_1_7

        repositories {
            jcenter()
            google()
        }

        group='com.github.triplet.play.test'
        version='1'

        dependencies {
            implementation 'com.android.tools.build:gradle:$pluginVersion'
            implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.2.41'
        }
        """)

        GradleRunner.create()
                .withPluginClasspath()
                .withGradleDistribution(URI(GRADLE_DIST))
                .withProjectDir(pluginProject.projectDir)
                .withArguments("assemble")
                .build()

        val pluginJar = File(pluginProject.buildDir, "/libs")
                .listFiles()
                .first { it.name.endsWith("jar") }
                .absolutePath

        File(testProject.projectDir, "build.gradle").writeText("""
        buildscript {
            repositories {
                google()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:$appVersion'
                classpath files("$pluginJar")
            }
        }

        apply plugin: 'com.android.application'
        apply plugin: 'com.github.triplet.play.test'

        android {
            compileSdkVersion 27
            buildToolsVersion '27.0.3'
            lintOptions {
                abortOnError false
            }

            defaultConfig {
                applicationId "de.triplet.gradle.play.test"
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
        GradleRunner.create()
                .withPluginClasspath()
                .withGradleDistribution(URI(GRADLE_DIST))
                .withProjectDir(pluginProject.projectDir)
                .withArguments("clean")
                .build()

        return true
    }

    @After
    fun cleanup() {
        File(kotlinProject.projectDir, "build.gradle").delete()
        File(groovyProject.projectDir, "build.gradle").delete()
        File(testProject.projectDir, "build.gradle").delete()
    }

    private companion object {
        const val GRADLE_DIST = "https://services.gradle.org/distributions/gradle-4.8-rc-1-all.zip"

        val agpVersions = arrayOf("2.2.3", "2.3.0", "3.0.1", "3.1.0", "3.2.0-alpha15")
    }
}
