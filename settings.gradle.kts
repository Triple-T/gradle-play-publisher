include(
        ":common:validation",

        ":play:plugin", ":play:publishing"
)

// The test app can only be used in development since it requires running
// ./gradlew publishToMavenLocal to work.
if (file("version.txt").readText().trim().contains("snapshot", true)) {
    includeBuild("testapp")
}
