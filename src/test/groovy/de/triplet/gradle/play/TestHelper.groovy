package de.triplet.gradle.play

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

public class TestHelper {

    public static final File FIXTURE_WORKING_DIR = new File("src/test/fixtures/android_app")

    public static Project evaluatableProject() {
        Project project = ProjectBuilder.builder().withProjectDir(FIXTURE_WORKING_DIR).build()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'com.github.triplet.play'
        project.android {
            compileSdkVersion 22
            buildToolsVersion '22.0.1'

            defaultConfig {
                versionCode 1
                versionName '1.0'
                minSdkVersion 22
                targetSdkVersion 22
            }

            buildTypes {
                release {
                    signingConfig signingConfigs.debug
                }
            }
        }

        return project
    }

    public static Project noSigningConfigProject() {
        Project project = ProjectBuilder.builder().withProjectDir(FIXTURE_WORKING_DIR).build()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'com.github.triplet.play'
        project.android {
            compileSdkVersion 22
            buildToolsVersion '22.0.1'

            defaultConfig {
                versionCode 1
                versionName '1.0'
                minSdkVersion 22
                targetSdkVersion 22
            }

            buildTypes {
                release {
                }
            }
        }

        return project
    }
}
