plugins {
    id("org.jetbrains.kotlin.jvm") version "1.2.41"
    id("groovy")
    id("java-gradle-plugin")
}
apply(from = "gradle-mvn-push.gradle")

repositories {
    google()
    jcenter()
}

dependencies {
    compile("com.android.tools.build:gradle:3.0.1")
    compile("com.google.apis:google-api-services-androidpublisher:v2-rev77-1.23.0") {
        exclude("com.google.guava", "guava-jdk5") // Remove when upgrading to AGP 3.1+
    }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.2.41")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.2.41")
    testImplementation("junit:junit:4.12")
    testImplementation("org.assertj:assertj-core:3.6.2")
    testImplementation("org.mockito:mockito-core:1.10.19")
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
