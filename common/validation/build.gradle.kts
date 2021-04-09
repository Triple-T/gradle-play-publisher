plugins {
    `kotlin-dsl`
    `maven-publish`
    signing
}

dependencies {
    compileOnly(Config.Libs.All.agp)

    testImplementation(Config.Libs.All.junit)
    testImplementation(Config.Libs.All.junitEngine)
    testImplementation(Config.Libs.All.truth)
    testImplementation(Config.Libs.All.agp)
}

afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        artifactId = "common-validation"
        configurePom()
        signing.sign(this)
    }
}
