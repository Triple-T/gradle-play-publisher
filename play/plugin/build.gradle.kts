plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.11.0"
}

dependencies {
    // Implementation dependencies, but compile-only so we don't add a POM dependency to those
    // internal projects. The libs are manually injected in the Jar task below.
    compileOnly(project(":play:android-publisher"))
    compileOnly(project(":common:utils"))
    compileOnly(project(":common:validation"))
    // START transitive deps - these get added to the POM
    runtimeOnly(Config.Libs.All.ap)
    runtimeOnly(Config.Libs.All.googleClient)
    // END

    compileOnly(Config.Libs.All.agp) // Compile only to not force a specific AGP version
    implementation(Config.Libs.All.guava)
    implementation(Config.Libs.All.jackson)

    testImplementation(project(":common:utils"))
    testImplementation(project(":common:validation"))
    testImplementation(testFixtures(project(":play:android-publisher")))
    testImplementation(Config.Libs.All.agp)

    testImplementation(Config.Libs.All.junit)
    testImplementation(Config.Libs.All.truth)
}

java {
    withJavadocJar()
    withSourcesJar()
}

// We want to be able to organize our project into multiple modules, but that typically requires
// publishing each one as a maven artifact. To get around this, we manually inject all locally
// produced libs into the final JAR.
tasks.withType<Jar>().configureEach {
    dependsOn(":play:android-publisher:processResources")

    val config = configurations.compileClasspath.get()
    val projectLibs = config.filter {
        it.path.contains(rootProject.layout.projectDirectory.asFile.path)
    }

    from(projectLibs.elements.map {
        it.flatMap {
            val f = it.asFile
            val variant = f.name
            val buildDir = f.parentFile.parentFile.parentFile

            listOf(it, File(buildDir, "resources/$variant"))
        }
    })
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

val versionName = rootProject.file("version.txt").readText().trim()
group = "com.github.triplet.gradle"
version = versionName

tasks.withType<PublishToMavenRepository>().configureEach {
    isEnabled = versionName.contains("snapshot", true)
}

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
