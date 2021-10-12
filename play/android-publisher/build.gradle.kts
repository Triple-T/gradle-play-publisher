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

    testImplementation(testLibs.junit)
    testImplementation(testLibs.junit.engine)
    testImplementation(testLibs.truth)
    testImplementation(testLibs.mockito)
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
