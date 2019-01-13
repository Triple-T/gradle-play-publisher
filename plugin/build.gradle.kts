import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    groovy
    id("com.gradle.plugin-publish") version "0.10.0"
}

dependencies {
    compileOnly("com.android.tools.build:gradle:3.4.0-alpha10")

    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev46-1.25.0")

    testImplementation("com.android.tools.build:gradle:3.4.0-alpha10")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.23.4")
    testImplementation("org.assertj:assertj-core:3.11.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

tasks.withType<KotlinJvmCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
    }
}

tasks.withType<ValidateTaskProperties>().configureEach {
    enableStricterValidation = true
    failOnWarning = true
}

tasks.named<Delete>("clean") {
    val base = "src/test/fixtures/android_app/"
    delete(base + ".gradle", base + "build", base + "userHome")
}

group = "com.github.triplet.gradle"
version = "2.1.0-SNAPSHOT"

gradlePlugin {
    plugins.create("play") {
        id = "com.github.triplet.play"
        displayName = "Gradle Play Publisher"
        description = "Gradle Play Publisher allows you to upload your App Bundle or APK " +
                "and other app details to the Google Play Store."
        implementationClass = "com.github.triplet.gradle.play.PlayPublisherPlugin"
    }
}

pluginBundle {
    website = "https://github.com/Triple-T/gradle-play-publisher"
    vcsUrl = "https://github.com/Triple-T/gradle-play-publisher"
    tags = listOf("android", "google-play")

    mavenCoordinates {
        groupId = project.group as String
        artifactId = "play-publisher"
    }
}

publishing {
    repositories {
        maven {
            name = "Snapshots"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")

            credentials {
                username = project.findProperty("SONATYPE_NEXUS_USERNAME")?.toString()
                password = project.findProperty("SONATYPE_NEXUS_PASSWORD")?.toString()
            }
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allSource)
    dependsOn(tasks["classes"])
}

afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        artifactId = "play-publisher"
        artifact(sourcesJar.get())

        pom {
            name.set("Google Play Publisher")
            description.set("Gradle Play Publisher is a plugin that allows you to upload your " +
                                    "App Bundle or APK and other app details to the " +
                                    "Google Play Store.")
            url.set("https://github.com/Triple-T/gradle-play-publisher")

            licenses {
                license {
                    name.set("The MIT License (MIT)")
                    url.set("http://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("bhurling")
                    name.set("Björn Hurling")
                    roles.set(listOf("Owner"))
                    timezone.set("+2")
                }
                developer {
                    id.set("SUPERCILEX")
                    name.set("Alex Saveau")
                    email.set("saveau.alexandre@gmail.com")
                    roles.set(listOf("Developer"))
                    timezone.set("-8")
                }
                developer {
                    id.set("ChristianBecker")
                    name.set("Christian Becker")
                    email.set("christian.becker.1987@gmail.com")
                    roles.set(listOf("Developer"))
                    timezone.set("+2")
                }
                developer {
                    id.set("gtcompscientist")
                    name.set("Charles Anderson")
                    roles.set(listOf("Developer"))
                    timezone.set("-8")
                }
            }

            scm {
                connection.set("scm:git@github.com:Triple-T/gradle-play-publisher.git")
                developerConnection.set("scm:git@github.com:Triple-T/gradle-play-publisher.git")
                url.set("https://github.com/Triple-T/gradle-play-publisher")
            }
        }
    }
}

tasks.withType<Test> {
    // Our integration tests need a fully compiled jar
    dependsOn("assemble")

    // Those tests also need to know which version was built
    systemProperty("VERSION_NAME", version)

    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}
