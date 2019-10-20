import org.gradle.api.artifacts.dsl.RepositoryHandler

fun RepositoryHandler.deps() {
    google().content {
        includeGroupByRegex("com\\.android\\..*")
        includeGroupByRegex("com\\.google\\..*")
        includeGroupByRegex("androidx\\..*")

        includeGroup("com.android")
        includeGroup("com.crashlytics.sdk.android")
        includeGroup("io.fabric.sdk.android")
    }

    jcenter()
}

object Config {
    object Libs {
        object All {
            const val agp = "com.android.tools.build:gradle:3.6.0-alpha12"
            const val ap =
                    "com.google.apis:google-api-services-androidpublisher:v3-rev20190910-1.30.1"

            const val junit = "junit:junit:4.13-beta-3"
            const val truth = "com.google.truth:truth:1.0"
            const val mockito = "org.mockito:mockito-core:3.1.0"
        }
    }
}
