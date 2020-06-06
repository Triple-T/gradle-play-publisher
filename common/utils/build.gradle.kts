plugins {
    `kotlin-dsl`
    `maven-publish`
    signing
    id("de.marcphilipp.nexus-publish")
}

afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        artifactId = "common-utils"
        configurePom()
        signing.sign(this)
    }
}
