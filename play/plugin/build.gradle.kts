plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    groovy
    id("com.gradle.plugin-publish") version "0.10.1"
}

dependencies {
    implementation(project(":common:validation", "default"))
    implementation(project(":play:android-publisher", "default"))

    compileOnly(Config.Libs.All.agp)
    implementation(Config.Libs.All.ap)

    testImplementation(Config.Libs.All.junit)
    testImplementation(kotlin("test"))
    testImplementation(Config.Libs.All.truth)
    testImplementation(Config.Libs.All.agp)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// We want to be able to organize our project into multiple modules, but that typically requires
// publishing each one as a maven artifact. To get around this, we manually inject all locally
// produced libs into the final JAR.
tasks.withType<Jar>().configureEach {
    val config = configurations.compileClasspath.get()
    val projectLibs = config.filter {
        it.path.contains(rootProject.layout.projectDirectory.asFile.path)
    }

    from(projectLibs.map { zipTree(it) })
}

tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.setFrom(/* reset */)

    pluginClasspath.from(configurations.compileClasspath)
    pluginClasspath.from(sourceSets.main.get().runtimeClasspath)
}

tasks.withType<ValidateTaskProperties>().configureEach {
    enableStricterValidation = true
    failOnWarning = true
}

tasks.named<Delete>("clean") {
    val deletables = listOf(".gradle", "build", "userHome")
    delete(deletables.map { "src/test/fixtures/android_app/$it" })
}

tasks.named("test") {
    inputs.files("src/test/fixtures/android_app/src")
}

group = "com.github.triplet.gradle"
version = rootProject.file("version.txt").readText().trim()

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
                username = System.getenv("SONATYPE_NEXUS_USERNAME")
                password = System.getenv("SONATYPE_NEXUS_PASSWORD")
            }
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
    dependsOn("classes")
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
}
