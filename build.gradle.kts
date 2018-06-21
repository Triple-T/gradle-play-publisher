buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.50")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.2.0")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.17.0"
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}
