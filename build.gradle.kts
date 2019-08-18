buildscript {
    repositories {
        google()
        jcenter()
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.22.0"
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register<Delete>("clean") {
    delete("build")
}

tasks.register("ciBuild") {
    val isMaster = System.getenv("CIRCLE_BRANCH") == "master"
    val isPr = System.getenv("CIRCLE_PULL_REQUEST") != null

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
