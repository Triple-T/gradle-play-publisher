import com.android.build.api.dsl.ApplicationExtension
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.common.utils.orNull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.google.common.hash.Hashing
import com.google.common.io.Files
import java.io.FileInputStream
import java.util.*

buildscript {
    repositories {
        mavenLocal()
        maven("https://oss.sonatype.org/content/repositories/snapshots")

        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath(kotlin("gradle-plugin", embeddedKotlinVersion))
        classpath("com.android.tools.build:gradle:9.0.0")
        classpath(
            "com.github.triplet.gradle:play-publisher:" +
                    file("../version.txt").readText().trim()
        )
    }
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"

        publishing.onlyIf { true }
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

apply(plugin = "com.android.application")
apply(plugin = "com.github.triplet.play")

the<JavaPluginExtension>().toolchain.languageVersion.convention(JavaLanguageVersion.of(8))

configure<ApplicationExtension> {
    namespace = "com.supercilex.test"
    compileSdk = 31

    defaultConfig {
        minSdk = 31
        targetSdk = 31
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
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }

        register("debugPlay") {
            isDebuggable = true
            versionNameSuffix = "-DEBUG"
        }

        release {
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

    lint {
        abortOnError = false
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

dependencies {
    "implementation"(kotlin("stdlib-jdk8", embeddedKotlinVersion))
    "implementation"("androidx.appcompat:appcompat:1.6.1")
    "implementation"("androidx.constraintlayout:constraintlayout:2.1.4")
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
                      .task(":play:play-publisher:publishToMavenLocal"))
}
tasks.matching { it.name != "validateBuildReadiness" }.configureEach { dependsOn(ready) }
