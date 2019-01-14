buildscript {
    repositories {
        google()
        jcenter()
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.20.0"
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register<Delete>("clean") {
    delete("build")
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}
