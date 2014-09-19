package de.triplet.gradle.play

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

public class TestHelper {

    public static final File FIXTURE_WORKING_DIR = new File("src/test/fixtures/android_app")

    public static Project evaluatableProject() {
        Project project = ProjectBuilder.builder().withProjectDir(FIXTURE_WORKING_DIR).build();
        project.apply plugin: 'android'
        project.apply plugin: 'play'
        project.android {
            compileSdkVersion 20
            buildToolsVersion '20.0.0'

            defaultConfig {
                versionCode 1
                versionName "1.0"
                minSdkVersion 20
                targetSdkVersion 20
            }

            buildTypes {
                release {
                    signingConfig signingConfigs.debug
                }
            }
        }

        return project
    }
}
