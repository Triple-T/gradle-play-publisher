buildscript {
    repositories {
        google()
        jcenter()
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.21.0"
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register<Delete>("clean") {
    delete("build")
}

println(System.getenv("SECRETS"))

tasks.register("ciBuild") {
    val isMaster = System.getenv("TRAVIS_BRANCH") == "master"
    val isPr = System.getenv("TRAVIS_PULL_REQUEST") ?: "false" != "false"

    if (isMaster && !isPr) { // Release build
        dependsOn(":plugin:build", ":plugin:publish")
    } else {
        dependsOn(":plugin:check")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}
