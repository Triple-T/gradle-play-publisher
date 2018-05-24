package de.triplet.gradle.play

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.StringWriter

class AndroidDefaultConfigTest {
    private lateinit var pluginProject: Project
    private lateinit var testProject: Project

    @Before
    fun setup() {
        pluginProject = ProjectBuilder.builder()
                .withProjectDir(File("src/test/fixtures/plugin"))
                .build()
        testProject = ProjectBuilder.builder()
                .withProjectDir(File("src/test/fixtures/android_app"))
                .build()
    }

    @After
    fun teardown() {
        File(pluginProject.projectDir, "build.gradle").delete()
        File(testProject.projectDir, "build.gradle").delete()
    }

    @Test(expected = org.gradle.testkit.runner.UnexpectedBuildFailure::class)
    fun `test android plugin 2_3_0 fails to find defaultConfig`() {
        pluginTest("2.3.0")
    }

    @Test
    fun `test android plugin 3_1_0 succeeds finding defaultConfig`() {
        pluginTest("3.1.0")
    }

    private fun pluginTest(pluginVersion: String) {
        File(pluginProject.projectDir, "build.gradle").writeText("""
        |plugins {
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
                .withGradleVersion("4.4")
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
        |        classpath 'com.android.tools.build:gradle:3.1.0'
        |        classpath files("$pluginJar")
        |    }
        |}
        |
        |apply plugin: 'com.android.application'
        |apply plugin: 'com.github.triplet.play.test'
        |
        |android {
        |    lintOptions {
        |        abortOnError false
        |    }
        |
        |    compileSdkVersion 27
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
                .withGradleVersion("4.4")
                .withProjectDir(testProject.projectDir)
                .withArguments("tasks")
                .build()

        GradleRunner.create()
                .withPluginClasspath()
                .withGradleVersion("4.4")
                .withProjectDir(testProject.projectDir)
                .withArguments("clean")
                .build()
        GradleRunner.create()
                .withPluginClasspath()
                .withGradleVersion("4.4")
                .withProjectDir(pluginProject.projectDir)
                .withArguments("clean")
                .build()

    }
}