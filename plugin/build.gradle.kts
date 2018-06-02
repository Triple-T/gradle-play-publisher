plugins {
    id("org.jetbrains.kotlin.jvm")
    id("groovy")
    id("java-gradle-plugin")
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation("com.android.tools.build:gradle:3.0.1")
    implementation("com.google.apis:google-api-services-androidpublisher:v2-rev77-1.23.0") {
        exclude("com.google.guava", "guava-jdk5") // Remove when upgrading to AGP 3.1+
    }
    implementation(kotlin("stdlib-jdk7"))

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.18.3")
    testImplementation("org.assertj:assertj-core:3.6.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

gradlePlugin {
    plugins {
        create("play") {
            id = "com.github.triplet.play"
            implementationClass = "de.triplet.gradle.play.PlayPublisherPlugin"
        }
    }
}
