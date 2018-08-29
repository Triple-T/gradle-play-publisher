buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.2.0")
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
