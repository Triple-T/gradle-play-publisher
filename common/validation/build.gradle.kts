plugins {
    `kotlin-dsl`
    `maven-publish`
    signing
}

dependencies {
    compileOnly(libs.agp)
    compileOnly(libs.agp.common)

    testImplementation(testLibs.junit)
    testImplementation(testLibs.junit.engine)
    testImplementation(testLibs.truth)
    testImplementation(libs.agp)
}

afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        artifactId = "common-validation"
        configurePom()
        signing.sign(this)
    }
}
