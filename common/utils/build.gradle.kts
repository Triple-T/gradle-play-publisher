plugins {
    `kotlin-dsl`
    `maven-publish`
    signing
}

afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        artifactId = "common-utils"
        configurePom()
        signing.sign(this)
    }
}
