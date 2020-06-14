import de.marcphilipp.gradle.nexus.NexusPublishExtension
import io.codearte.gradle.nexus.CloseRepositoryTask
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

buildscript {
    repositories.deps()

    dependencies {
        classpath(kotlin("gradle-plugin", embeddedKotlinVersion))
    }
}

plugins {
    `lifecycle-base`
    id("com.github.ben-manes.versions") version "0.28.0"

    // Needed to support publishing all modules atomically
    id("de.marcphilipp.nexus-publish") version "0.4.0" apply false
    // Needed to deploy library releases
    id("io.codearte.nexus-staging") version "0.21.2"
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

nexusStaging {
    packageGroup = "com.github.triplet"
    username = System.getenv("SONATYPE_NEXUS_USERNAME")
    password = System.getenv("SONATYPE_NEXUS_PASSWORD")

    // 15 minutes
    delayBetweenRetriesInMillis = 5_000
    numberOfRetries = 180
}

tasks.withType<CloseRepositoryTask> {
    mustRunAfter(allprojects.map {
        it.tasks.matching { task ->
            task.name.contains("publishToSonatype")
        }
    })
}

val versionName = rootProject.file("version.txt").readText().trim()
allprojects {
    repositories.deps()

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

        convention.findByType<NexusPublishExtension>()?.apply {
            repositories {
                sonatype {
                    username.set(System.getenv("SONATYPE_NEXUS_USERNAME"))
                    password.set(System.getenv("SONATYPE_NEXUS_PASSWORD"))
                }
            }
        }

        convention.findByType<SigningExtension>()?.apply {
            isRequired = false

            useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        maxHeapSize = "2g"
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
