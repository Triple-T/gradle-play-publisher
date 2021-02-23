plugins {
    `gradle-enterprise`
}

include(
        ":common:utils", ":common:validation",

        ":play:plugin", ":play:android-publisher"
)

dependencyResolutionManagement {
    repositories {
        google().content {
            includeGroup("com.android")
            includeGroupByRegex("com\\.android\\..*")
            includeGroupByRegex("com\\.google\\..*")
            includeGroupByRegex("androidx\\..*")
        }

        mavenCentral()
        jcenter()
    }
}
