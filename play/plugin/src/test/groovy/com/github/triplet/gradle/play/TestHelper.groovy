package com.github.triplet.gradle.play

import com.github.triplet.gradle.common.utils.IoKt
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

class TestHelper {

    static final FIXTURES_DIR = new File('src/test/fixtures')
    static final FIXTURE_WORKING_DIR = new File(FIXTURES_DIR, 'android_app')
    private static final BUILD_FILE = new File(FIXTURE_WORKING_DIR, "build.gradle")

    static Project fixtureProject() {
        def project = ProjectBuilder.builder().withProjectDir(FIXTURE_WORKING_DIR).build()

        def base = new File(project.buildDir, "outputs/apk")
        IoKt.safeCreateNewFile(new File(base, "release/test-release.apk")).write("")
        IoKt.safeCreateNewFile(new File(base, "paid/release/test-paid-release.apk")).write("")

        return project
    }

    static Project evaluatableProject() {
        def project = fixtureProject()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'com.github.triplet.play'
        project.android {
            compileSdkVersion 28

            defaultConfig {
                versionCode 1
                versionName '1.0'
                minSdkVersion 28
                targetSdkVersion 28
            }

            buildTypes {
                release {
                    signingConfig signingConfigs.debug
                }
            }
        }
        project.play {
            serviceAccountCredentials = new File("fake.json")
        }

        return project
    }

    static BuildResult execute(String androidConfig, String... tasks) {
        execute(androidConfig, false, tasks)
    }

    static BuildResult execute(String androidConfig, boolean expectFailure, String... tasks) {
        // language=gradle
        BUILD_FILE.write("""
        buildscript {
            repositories {
                google()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:3.6.0-alpha11'
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

            ${androidConfig}
        }

        play {
            serviceAccountCredentials = file('some-file.json')
        }
        """)

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(FIXTURE_WORKING_DIR)
                .withArguments(tasks)

        try {
            if (expectFailure) return runner.buildAndFail() else return runner.build()
        } finally {
            BUILD_FILE.delete()
        }
    }
}
