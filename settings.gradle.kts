plugins {
    `gradle-enterprise`
}

include(
    ":common:utils", ":common:validation",

    ":play:plugin", ":play:android-publisher"
)

// sets name explicitly to make sure it uses the in intended `artifactId` in the plugin POM
project(":play:plugin").name = "play-publisher"

dependencyResolutionManagement {
    repositories {
        google().content {
            includeGroup("com.android")
            includeGroupByRegex("com\\.android\\..*")
            includeGroupByRegex("com\\.google\\..*")
            includeGroupByRegex("androidx\\..*")
        }

        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            version("depUpdates", "0.50.0")
            version("gradlePublish", "1.2.1")
            version("nexusPublish", "1.3.0")

            plugin("depUpdates", "com.github.ben-manes.versions")
                .versionRef("depUpdates")
            plugin("gradlePublish", "com.gradle.plugin-publish")
                .versionRef("gradlePublish")
            plugin("nexusPublish", "io.github.gradle-nexus.publish-plugin")
                .versionRef("nexusPublish")

            version("agp", "8.0.1")
            version("agp-tools", "31.0.1")
            version("android-publisher", "v3-rev20231115-2.0.0")
            version("api-client", "2.2.0")
            version("http-client", "1.43.3")
            version("http-auth", "1.20.0")
            version("guava", "31.1-jre")

            library("agp", "com.android.tools.build", "gradle").versionRef("agp")
            library("agp-test", "com.android.tools.build", "builder-test-api").versionRef("agp")
            library("agp-common", "com.android.tools", "common").versionRef("agp-tools")
            library("agp-ddms", "com.android.tools.ddms", "ddmlib").versionRef("agp-tools")
            library("androidpublisher", "com.google.apis", "google-api-services-androidpublisher")
                .versionRef("android-publisher")
            library("client-api", "com.google.api-client", "google-api-client")
                .versionRef("api-client")
            library("client-http", "com.google.http-client", "google-http-client-apache-v2")
                .versionRef("http-client")
            library("client-auth", "com.google.auth", "google-auth-library-oauth2-http")
                .versionRef("http-auth")
            library("client-gson", "com.google.http-client", "google-http-client-gson")
                .versionRef("http-client")
            library("guava", "com.google.guava", "guava").versionRef("guava")
        }

        create("testLibs") {
            version("junit", "5.10.1")
            version("truth", "1.1.3")
            version("mockito", "5.8.0")

            library("junit", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
            library("junit-engine", "org.junit.jupiter", "junit-jupiter-engine")
                .versionRef("junit")
            library("junit-params", "org.junit.jupiter", "junit-jupiter-params")
                .versionRef("junit")
            library("truth", "com.google.truth", "truth").versionRef("truth")
            library("mockito", "org.mockito", "mockito-core").versionRef("mockito")
        }
    }
}
