plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.10.1"
}

dependencies {
    compileOnly(project(":play:android-publisher", "default"))
    // START transitive deps
    compileOnly(project(":common:validation", "default"))
    runtimeOnly(Config.Libs.All.ap)
    runtimeOnly(Config.Libs.All.googleClient)
    // END

    compileOnly(Config.Libs.All.agp)
    implementation(Config.Libs.All.guava)

    testImplementation(project(":common:utils", "default"))
    testImplementation(project(":common:validation", "default"))
    testImplementation(testFixtures(project(":play:android-publisher")))
    testImplementation(Config.Libs.All.agp)

    testImplementation(Config.Libs.All.junit)
    testImplementation(Config.Libs.All.truth)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withJavadocJar()
    withSourcesJar()
}

// We want to be able to organize our project into multiple modules, but that typically requires
// publishing each one as a maven artifact. To get around this, we manually inject all locally
// produced libs into the final JAR.
tasks.withType<Jar>().configureEach {
    val config = configurations.compileClasspath.get()
    val projectLibs = config.filter {
        it.path.contains(rootProject.layout.projectDirectory.asFile.path)
    }

    from(projectLibs.elements.map { it.map { zipTree(it) } })
}

tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.setFrom(/* reset */)

    pluginClasspath.from(configurations.compileClasspath)
    pluginClasspath.from(configurations.testCompileClasspath)
    pluginClasspath.from(sourceSets.main.get().runtimeClasspath)
}

tasks.withType<ValidatePlugins>().configureEach {
    enableStricterValidation.set(true)
}

tasks.named("test") {
    inputs.files(fileTree("src/test/fixtures"))
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

afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        artifactId = "play-publisher"

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
                    id.set("SUPERCILEX")
                    name.set("Alex Saveau")
                    email.set("saveau.alexandre@gmail.com")
                    roles.set(listOf("Owner"))
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
