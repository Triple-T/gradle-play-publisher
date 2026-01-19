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

    testFixturesImplementation(libs.client.api)
    testFixturesImplementation(libs.client.gson)

    testRuntimeOnly(testLibs.junit.launcher)
    testImplementation(testLibs.junit)
    testImplementation(testLibs.junit.engine)
    testImplementation(testLibs.truth)
    testImplementation(testLibs.mockito)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}

tasks.test.configure {
    useJUnitPlatform()
}

// Mockito needs to be able to pass in null params
tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions {
        freeCompilerArgs.add("-Xno-call-assertions")
    }
}

afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        artifactId = "android-publisher"
        configurePom()
        signing.sign(this)
    }
}
