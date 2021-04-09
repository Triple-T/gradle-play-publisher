import io.github.gradlenexus.publishplugin.CloseNexusStagingRepository
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import java.time.Duration

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", embeddedKotlinVersion))
    }
}

plugins {
    `lifecycle-base`
    id("com.github.ben-manes.versions") version "0.38.0"

    // Needed to support publishing all modules atomically
    id("com.gradle.plugin-publish") version "0.14.0" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishAlways()
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
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

    afterEvaluate {
        convention.findByType<JavaPluginExtension>()?.apply {
            sourceCompatibility = JavaVersion.VERSION_1_8
            withJavadocJar()
            withSourcesJar()
        }

        convention.findByType<KotlinProjectExtension>()?.apply {
            sourceSets.configureEach {
                languageSettings.progressiveMode = true
                languageSettings.enableLanguageFeature("NewInference")
            }
        }

        convention.findByType<PublishingExtension>()?.apply {
            configureMaven(repositories)
        }

        convention.findByType<SigningExtension>()?.apply {
            isRequired = false

            useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
        }
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
