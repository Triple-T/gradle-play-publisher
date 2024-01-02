import com.gradle.publish.PublishTaskShadowAction
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-test-fixtures`
    `maven-publish`
    signing
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":common:utils"))
    shadowImplementation(libs.androidpublisher)
    shadowImplementation(libs.client.api)
    shadowImplementation(libs.client.auth)
    shadowImplementation(libs.client.http)

    testImplementation(testLibs.junit)
    testImplementation(testLibs.junit.engine)
    testImplementation(testLibs.truth)
    testImplementation(testLibs.mockito)
}

// Normally performed by plugin-publish
pluginManager.withPlugin(
    "com.github.johnrengelman.shadow",
    PublishTaskShadowAction(project, logger)
)

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
