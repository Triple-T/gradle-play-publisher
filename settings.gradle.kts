plugins {
    `gradle-enterprise`
}

include(
        ":common:utils", ":common:validation",

        ":play:plugin", ":play:android-publisher"
)
