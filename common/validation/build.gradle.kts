plugins {
    java
    `kotlin-dsl`
}

dependencies {
    compileOnly(Config.Libs.All.agp)

    testImplementation(Config.Libs.All.junit)
    testImplementation(kotlin("test"))
    testImplementation(Config.Libs.All.truth)
    testImplementation(Config.Libs.All.agp)
}
