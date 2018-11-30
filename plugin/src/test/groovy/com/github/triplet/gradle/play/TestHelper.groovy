package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.IoKt
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection

class TestHelper {

    static final FIXTURE_WORKING_DIR = new File('src/test/fixtures/android_app')
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
            compileSdkVersion 27

            defaultConfig {
                versionCode 1
                versionName '1.0'
                minSdkVersion 27
                targetSdkVersion 27
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

    static ProjectConnection generateConnection(String androidConfig) {
        // language=gradle
        BUILD_FILE.write("""
        buildscript {
            repositories {
                google()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:3.2.1'
                classpath files('../../../../build/libs/plugin-${System.getProperty("VERSION_NAME")}.jar')

                // Manually define transitive dependencies for our plugin since we don't have the
                // POM to fetch them for us
                classpath('com.google.apis:google-api-services-androidpublisher:v3-rev12-1.23.0')
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

        return GradleConnector.newConnector().forProjectDirectory(FIXTURE_WORKING_DIR).connect()
    }

    static void executeConnection(ProjectConnection connection, String task) {
        try {
            connection.newBuild().forTasks(task).run()
        } catch (BuildException e) {
            throw e.cause.cause.cause
        }
    }

    static void cleanup() {
        BUILD_FILE.delete()
    }
}
