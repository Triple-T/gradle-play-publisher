import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-test-fixtures`
    `maven-publish`
    signing
}

dependencies {
    implementation(project(":common:utils"))
    implementation(Config.Libs.All.ap)
    implementation(Config.Libs.All.googleClient)
    implementation(Config.Libs.All.auth)
    implementation(Config.Libs.All.apacheClientV2)

    testImplementation(Config.Libs.All.junit)
    testImplementation(Config.Libs.All.junitEngine)
    testImplementation(Config.Libs.All.truth)
    testImplementation(Config.Libs.All.mockito)
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
