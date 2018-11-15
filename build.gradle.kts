buildscript {
    repositories {
        google()
        jcenter()
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.20.0"
}

tasks.withType<Wrapper>().configureEach {
    distributionType = Wrapper.DistributionType.ALL
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}
