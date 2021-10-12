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

    compileOnly(libs.agp) // Compile only to not force a specific AGP version
    compileOnly(libs.agp.common)
    compileOnly(libs.agp.test)
    compileOnly(libs.agp.ddms)
    implementation(libs.guava)
    implementation(libs.client.gson)

    testImplementation(project(":common:utils"))
    testImplementation(project(":common:validation"))
    testImplementation(testFixtures(project(":play:android-publisher")))
    testImplementation(libs.agp)

    testImplementation(testLibs.junit)
    testImplementation(testLibs.junit.engine)
    testImplementation(testLibs.junit.params)
    testImplementation(testLibs.truth)
}

tasks.withType<PluginUnderTestMetadata>().configureEach {
    dependsOn("compileKotlin", "compileTestKotlin", "compileJava", "compileTestJava")
    dependsOn("processResources", "processTestResources")
    dependsOn(":play:android-publisher:testFixturesJar")

    pluginClasspath.setFrom(/* reset */)

    pluginClasspath.from(configurations.compileClasspath)
    pluginClasspath.from(configurations.testCompileClasspath)
    pluginClasspath.from(configurations.runtimeClasspath)
    pluginClasspath.from(provider { sourceSets.test.get().runtimeClasspath.files })
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
