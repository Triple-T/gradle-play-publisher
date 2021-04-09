plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    signing
    id("com.gradle.plugin-publish")
}

dependencies {
    implementation(project(":play:android-publisher"))
    implementation(project(":common:utils"))
    implementation(project(":common:validation"))

    compileOnly(Config.Libs.All.agp) // Compile only to not force a specific AGP version
    implementation(Config.Libs.All.guava)
    implementation(Config.Libs.All.gson)

    testImplementation(project(":common:utils"))
    testImplementation(project(":common:validation"))
    testImplementation(testFixtures(project(":play:android-publisher")))
    testImplementation(Config.Libs.All.agp)

    testImplementation(Config.Libs.All.junit)
    testImplementation(Config.Libs.All.junitEngine)
    testImplementation(Config.Libs.All.junitParams)
    testImplementation(Config.Libs.All.truth)
}

tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.setFrom(/* reset */)

    pluginClasspath.from(configurations.compileClasspath)
    pluginClasspath.from(configurations.testCompileClasspath)
    pluginClasspath.from(sourceSets.main.get().runtimeClasspath)
}

afterEvaluate {
    tasks.withType<PublishToMavenRepository>().configureEach {
        isEnabled = isSnapshotBuild || publication.name == "pluginMaven"
    }
}

tasks.withType<Test> {
    inputs.files(fileTree("src/test/fixtures"))

    // Our integration tests need a fully compiled jar
    dependsOn("assemble")

    // Those tests also need to know which version was built
    systemProperty("VERSION_NAME", version)
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
    tags = listOf("android", "google-play", "publishing", "deployment", "apps", "mobile")

    mavenCoordinates {
        groupId = project.group as String
        artifactId = "play-publisher"
    }
}

afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        artifactId = "play-publisher"
        configurePom()
        signing.sign(this)
    }
}
