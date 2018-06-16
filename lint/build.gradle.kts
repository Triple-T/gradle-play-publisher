plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

android {
    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_7)
        setTargetCompatibility(JavaVersion.VERSION_1_7)
    }

    compileSdkVersion(27)

    defaultConfig {
        minSdkVersion(1)
    }

    // TODO: https://issuetracker.google.com/issues/72050365
    libraryVariants.all {
        generateBuildConfig?.enabled = false
    }
}

dependencies {
    lintChecks(project(":lint-rules"))
}
