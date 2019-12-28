import org.gradle.api.artifacts.dsl.RepositoryHandler

fun RepositoryHandler.deps() {
    google().content {
        includeGroup("com.android")
        includeGroupByRegex("com\\.android\\..*")
        includeGroupByRegex("com\\.google\\..*")
        includeGroupByRegex("androidx\\..*")
    }

    jcenter()
}

object Config {
    object Libs {
        object All {
            const val agp = "com.android.tools.build:gradle:4.0.0-alpha07"
            const val ap =
                    "com.google.apis:google-api-services-androidpublisher:v3-rev20190910-1.30.1"

            const val junit = "junit:junit:4.13-rc-2"
            const val truth = "com.google.truth:truth:1.0"
            const val mockito = "org.mockito:mockito-core:3.2.4"
        }
    }
}
