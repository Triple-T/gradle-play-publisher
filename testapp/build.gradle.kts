import com.android.build.gradle.BaseExtension
import com.github.triplet.gradle.play.PlayPublisherExtension
import java.io.FileInputStream
import java.util.Properties

buildscript {
    repositories {
        google()
        jcenter()
        mavenLocal()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", embeddedKotlinVersion))
        classpath("com.android.tools.build:gradle:3.5.1")
        classpath("com.github.triplet.gradle:play-publisher:" +
                          file("../version.txt").readText().trim())
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

apply(plugin = "com.android.application")
apply(plugin = "kotlin-android")
apply(plugin = "com.github.triplet.play")

configure<BaseExtension> {
    compileSdkVersion(29)

    defaultConfig {
        minSdkVersion(16)
        targetSdkVersion(29)
        versionCode = 1 // Updated on every build
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = file("keystore.properties")
            val keystoreProperties = Properties()
            keystoreProperties.load(FileInputStream(keystorePropertiesFile))

            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
    }

    lintOptions {
        isAbortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

configure<PlayPublisherExtension> {
    serviceAccountCredentials = file("google-play-auto-publisher.json")
    promoteTrack = "alpha"
    resolutionStrategy = "auto"
    outputProcessor { versionNameOverride = "$versionNameOverride.$versionCode" }
    defaultToAppBundles = true
}

dependencies {
    "implementation"(kotlin("stdlib-jdk8", embeddedKotlinVersion))
    "implementation"("androidx.appcompat:appcompat:1.1.0")
    "implementation"("androidx.multidex:multidex:2.0.1")
    "implementation"("androidx.constraintlayout:constraintlayout:1.1.3")
}
