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
    }

    enableFeaturePreview("VERSION_CATALOGS")
    versionCatalogs {
        create("libs") {
            version("agp", "7.0.0")
            version("agp-tools", "30.0.0")
            version("android-publisher", "v3-rev20210527-1.31.0")
            version("api-client", "1.31.5")
            version("http-client", "1.39.2")
            version("http-auth", "0.26.0")
            version("guava", "30.1.1-jre")

            alias("agp").to("com.android.tools.build", "gradle").versionRef("agp")
            alias("agp-test").to("com.android.tools.build", "builder-test-api").versionRef("agp")
            alias("agp-common").to("com.android.tools", "common").versionRef("agp-tools")
            alias("agp-ddms").to("com.android.tools.ddms", "ddmlib").versionRef("agp-tools")
            alias("androidpublisher")
                    .to("com.google.apis", "google-api-services-androidpublisher")
                    .versionRef("android-publisher")
            alias("client-api")
                    .to("com.google.api-client", "google-api-client")
                    .versionRef("api-client")
            alias("client-http")
                    .to("com.google.http-client", "google-http-client-apache-v2")
                    .versionRef("http-client")
            alias("client-auth")
                    .to("com.google.auth", "google-auth-library-oauth2-http")
                    .versionRef("http-auth")
            alias("client-gson")
                    .to("com.google.http-client", "google-http-client-gson")
                    .versionRef("http-client")
            alias("guava").to("com.google.guava", "guava").versionRef("guava")
        }

        create("testLibs") {
            version("junit", "5.7.0")
            version("truth", "1.1.3")
            version("mockito", "3.10.0")

            alias("junit").to("org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
            alias("junit-engine")
                    .to("org.junit.jupiter", "junit-jupiter-engine")
                    .versionRef("junit")
            alias("junit-params")
                    .to("org.junit.jupiter", "junit-jupiter-params")
                    .versionRef("junit")
            alias("truth").to("com.google.truth", "truth").versionRef("truth")
            alias("mockito").to("org.mockito", "mockito-core").versionRef("mockito")
        }
    }
}
