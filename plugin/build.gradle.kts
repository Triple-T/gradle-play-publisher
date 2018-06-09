plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.gradle.groovy")
    id("org.gradle.java-gradle-plugin")
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation("com.android.tools.build:gradle:3.0.1")
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev6-1.23.0") {
        exclude("com.google.guava", "guava-jdk5") // Remove when upgrading to AGP 3.1+
    }
    implementation(kotlin("stdlib-jdk7"))

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.18.3")
    testImplementation("org.assertj:assertj-core:3.10.0")
}

val versionOutputDir = "src/generated/kotlin"

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7

    sourceSets {
        "main" {
            java.srcDir(versionOutputDir)
        }
    }
}

gradlePlugin {
    plugins {
        create("play") {
            id = "com.github.triplet.play"
            implementationClass = "com.github.triplet.gradle.play.PlayPublisherPlugin"
        }
    }
}

tasks {
    "pluginVersion" {
        inputs.property("version", version)
        outputs.dir(versionOutputDir)

        doLast {
            val versionFile = file("$versionOutputDir/com/github/triplet/gradle/play/Version.kt")
            versionFile.parentFile.mkdirs()
            versionFile.writeText("""
                |// Generated file. Do not edit!
                |package com.github.triplet.gradle.play
                |
                |const val VERSION = "${project.version}"
                |""".trimMargin())
        }
    }

    "compileKotlin" {
        dependsOn("pluginVersion")
    }
}
