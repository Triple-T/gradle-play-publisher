import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.github.gradlenexus.publishplugin.CloseNexusStagingRepository
import java.time.Duration
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", embeddedKotlinVersion))
    }
}

plugins {
    `lifecycle-base`
    alias(libs.plugins.depUpdates)

    // Needed to support publishing all modules atomically
    alias(libs.plugins.gradlePublish) apply false
    alias(libs.plugins.nexusPublish)
    alias(libs.plugins.shadow) apply false
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishAlways()
}

tasks.register("configureGithubActions") {
    doLast {
        println("::set-output name=is_snapshot::$isSnapshotBuild")
    }
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_NEXUS_USERNAME"))
            password.set(System.getenv("SONATYPE_NEXUS_PASSWORD"))
        }
    }

    transitionCheckOptions {
        // 15 minutes
        delayBetween.set(Duration.ofSeconds(5))
        maxRetries.set(180)
    }
}

tasks.withType<CloseNexusStagingRepository> {
    mustRunAfter(allprojects.map {
        it.tasks.matching { task ->
            task.name.contains("publishToSonatype")
        }
    })
}

val versionName = rootProject.file("version.txt").readText().trim()
allprojects {
    version = versionName
    group = "com.github.triplet.gradle"

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            toolchain.languageVersion.convention(JavaLanguageVersion.of(11))
            withJavadocJar()
            withSourcesJar()
        }
    }

    plugins.withType<KotlinPluginWrapper> {
        configure<KotlinProjectExtension> {
            sourceSets.configureEach {
                languageSettings.progressiveMode = true
                languageSettings.enableLanguageFeature("NewInference")
            }
        }
    }

    plugins.withType<PublishingPlugin> {
        configure<PublishingExtension> {
            configureMaven(repositories)
        }
    }

    plugins.withType<SigningPlugin> {
        configure<SigningExtension> {
            isRequired = false

            useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
        }
    }

    plugins.withType<ShadowPlugin> {
        val shadowImplementation by configurations.creating
        configurations["compileOnly"].extendsFrom(shadowImplementation)
        configurations["testImplementation"].extendsFrom(shadowImplementation)

        tasks.withType<ShadowJar> {
            archiveClassifier.set("")
            configurations = listOf(shadowImplementation)
            isEnableRelocation = true
            relocationPrefix = "com.github.triplet.gradle.shaded"
        }
    }

    // Needed to preserve JAR hash for testapp build
    tasks.withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        maxHeapSize = "4g"
        systemProperty("junit.jupiter.execution.parallel.enabled", true)
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")

        testLogging {
            events("passed", "failed", "skipped")
            showStandardStreams = true
            setExceptionFormat("full")
        }
    }

    tasks.withType<ValidatePlugins>().configureEach {
        enableStricterValidation.set(true)
    }
}
