plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(project(":common:utils", "default"))
    implementation(Config.Libs.All.ap)

    testImplementation(kotlin("test"))
    testImplementation(Config.Libs.All.junit)
    testImplementation(Config.Libs.All.truth)
}
