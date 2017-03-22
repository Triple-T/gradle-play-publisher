package de.triplet.gradle.play

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class TestHelper {

    static final FIXTURE_WORKING_DIR = new File('src/test/fixtures/android_app')

    static Project fixtureProject() {
        return ProjectBuilder.builder().withProjectDir(FIXTURE_WORKING_DIR).build()
    }

    static Project evaluatableProject() {
        def project = fixtureProject()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'com.github.triplet.play'
        project.android {
            compileSdkVersion 25
            buildToolsVersion '25.0.2'

            defaultConfig {
                versionCode 1
                versionName '1.0'
                minSdkVersion 25
                targetSdkVersion 25
            }

            buildTypes {
                release {
                    signingConfig signingConfigs.debug
                }
            }
        }

        return project
    }

    static Project noSigningConfigProject() {
        def project = fixtureProject()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'com.github.triplet.play'
        project.android {
            compileSdkVersion 25
            buildToolsVersion '25.0.2'

            defaultConfig {
                versionCode 1
                versionName '1.0'
                minSdkVersion 25
                targetSdkVersion 25
            }

            buildTypes {
                release {
                }
            }
        }

        return project
    }
}
