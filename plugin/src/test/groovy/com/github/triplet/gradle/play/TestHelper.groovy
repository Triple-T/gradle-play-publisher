package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.IoKt
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class TestHelper {

    static final FIXTURE_WORKING_DIR = new File('src/test/fixtures/android_app')

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
}
