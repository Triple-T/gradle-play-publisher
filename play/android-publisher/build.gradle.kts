import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-test-fixtures`
}

dependencies {
    implementation(project(":common:utils", "default"))
    implementation(Config.Libs.All.ap)

    testImplementation(Config.Libs.All.junit)
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
