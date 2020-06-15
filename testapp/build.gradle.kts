import com.android.build.gradle.BaseExtension
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.supercilex.gradle.versions.VersionMasterExtension
import java.io.FileInputStream
import java.util.Properties

buildscript {
    repositories {
        google()
        jcenter()
        mavenLocal()
        maven("https://plugins.gradle.org/m2/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    dependencies {
        classpath(kotlin("gradle-plugin", embeddedKotlinVersion))
        classpath("com.android.tools.build:gradle:4.1.0-beta01")
        classpath("com.supercilex.gradle:version-master:1.0.0-SNAPSHOT")
        classpath("com.github.triplet.gradle:play-publisher:" +
                          file("../version.txt").readText().trim())
    }
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

apply(plugin = "com.android.application")
apply(plugin = "kotlin-android")
apply(plugin = "com.supercilex.gradle.versions")
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
        register("release") {
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
        named("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }

        register("debugPlay") {
            initWith(named("debug").get())
            applicationIdSuffix = null
        }

        named("release") {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    (this as ExtensionAware).extensions.configure<
            NamedDomainObjectContainer<PlayPublisherExtension>>("playConfigs") {
        register("debug") {
            enabled.set(false)
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
    serviceAccountCredentials.set(file("google-play-auto-publisher.json"))
    defaultToAppBundles.set(true)

    promoteTrack.set("alpha")
    resolutionStrategy.set(ResolutionStrategy.AUTO)
}

configure<VersionMasterExtension> {
    configureVersionCode.set(false)
}

dependencies {
    "implementation"(kotlin("stdlib-jdk8", embeddedKotlinVersion))
    "implementation"("androidx.appcompat:appcompat:1.1.0")
    "implementation"("androidx.multidex:multidex:2.0.1")
    "implementation"("androidx.constraintlayout:constraintlayout:1.1.3")
}

abstract class BuildReadinessValidator : DefaultTask() {
    @TaskAction
    fun validate() {
        if (project.hasProperty("skipValidation")) return

        val playChecksumFile = project.layout.buildDirectory
                .file("build-validator/play").get().asFile
        val playPlugin = File(project.rootDir.parentFile, "play/plugin/build/libs")

        val oldHashes = playChecksumFile.orNull()?.readLines().orEmpty().toSet()
        val newHashes = playPlugin.listFiles().orEmpty().map {
            Files.asByteSource(it).hash(Hashing.sha256()).toString()
        }.toSet()

        check(oldHashes == newHashes) {
            playChecksumFile.safeCreateNewFile().writeText(newHashes.joinToString("\n"))

            "Plugin updated. Rerun command to finish build."
        }
    }
}

val ready = tasks.register<BuildReadinessValidator>("validateBuildReadiness") {
    dependsOn(gradle.includedBuild("gradle-play-publisher")
                      .task(":play:plugin:publishToMavenLocal"))
}
tasks.matching { it.name != "validateBuildReadiness" }.configureEach { dependsOn(ready) }
