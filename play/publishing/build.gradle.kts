plugins {
    java
    `kotlin-dsl`
}

dependencies {
    implementation(Config.Libs.All.ap)

    testImplementation(Config.Libs.All.junit)
    testImplementation(kotlin("test"))
    testImplementation(Config.Libs.All.truth)
}
