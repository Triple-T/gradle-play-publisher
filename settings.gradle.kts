plugins {
    `gradle-enterprise`
}

include(
        ":common:utils", ":common:validation",

        ":play:plugin", ":play:android-publisher"
)

// Since the test app needs ./gradlew publishToMavenLocal to work, this enables bootstrapping it.
if (System.getProperty("bootstrap") == null) {
    includeBuild("testapp")
}
