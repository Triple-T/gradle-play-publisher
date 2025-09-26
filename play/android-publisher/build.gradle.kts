import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-test-fixtures`
    `maven-publish`
    signing
}

dependencies {
    implementation(project(":common:utils"))
    implementation(libs.androidpublisher)
    implementation(libs.client.api)
    implementation(libs.client.auth)
    implementation(libs.client.http)
    // Add Google API Client dependencies for testFixtures
    testFixturesImplementation("com.google.api-client:google-api-client:2.4.0")
    testFixturesImplementation("com.google.http-client:google-http-client-gson:1.43.3")

    testImplementation(testLibs.junit)
    testImplementation(testLibs.junit.engine)
    testImplementation(testLibs.truth)
    testImplementation(testLibs.mockito)
}

// Set Kotlin language version to 2.0 and disable progressive mode
// Applies to all KotlinCompile tasks
tasks.withType<KotlinCompile> {
    kotlinOptions {
        languageVersion = "2.0"
        freeCompilerArgs = freeCompilerArgs.filterNot { it == "-progressive" }
    }
}

// Mockito needs to be able to pass in null params
tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions {
        freeCompilerArgs += "-Xno-call-assertions"
    }
}

// Give testFixtures access to internal symbols
// TODO(asaveau): remove when https://youtrack.jetbrains.com/issue/KT-34901 gets fixed
kotlin.target.compilations {
    named("testFixtures") {
        associateWith(named("main").get())
    }
}

afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        artifactId = "android-publisher"
        configurePom()
        signing.sign(this)
    }
}
