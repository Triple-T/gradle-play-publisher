import org.ajoberstar.grgit.Grgit

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("groovy")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.9.10"
    id("org.ajoberstar.grgit") version "2.2.1"
}

dependencies {
    implementation("com.android.tools.build:gradle:3.0.1")
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev12-1.23.0") {
        exclude("com.google.guava", "guava-jdk5") // Remove when upgrading to AGP 3.1+
    }
    implementation(kotlin("stdlib-jdk7"))

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.18.3")
    testImplementation("org.assertj:assertj-core:3.10.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

gradlePlugin {
    plugins {
        create("play") {
            id = "com.github.triplet.play"
            implementationClass = "com.github.triplet.gradle.play.PlayPublisherPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/Triple-T/gradle-play-publisher"
    vcsUrl = "https://github.com/Triple-T/gradle-play-publisher"
    description = "Upload APKs and App Bundles to the Google Play Store"

    (plugins) {
        "playPublisherPlugin" {
            id = "com.github.triplet.play"
            displayName = "Gradle Play Publisher"
            tags = listOf("android", "google-play")
        }
    }

    mavenCoordinates {
        groupId = "com.github.triplet.gradle"
        artifactId = "play-publisher"
    }
}

afterEvaluate {
    tasks["jar"].dependsOn(task("applyVersion").doFirst {
        var pluginVersion = "2.0.0"

        if (System.getenv("TRAVIS_REPO_SLUG") != null) { // Improve local perf
            // Comment out to release stable version
            pluginVersion += "-" + Grgit.open {
                it.dir = rootProject.projectDir
            }.head().abbreviatedId
        }

        version = pluginVersion
    })
}
