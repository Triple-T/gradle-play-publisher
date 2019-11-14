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
            const val agp = "com.android.tools.build:gradle:3.6.0-beta02"
            const val ap =
                    "com.google.apis:google-api-services-androidpublisher:v3-rev20190910-1.30.1"
            const val gauthlib = "com.google.auth:google-auth-library-oauth2-http:0.18.0"

            const val junit = "junit:junit:4.13-rc-1"
            const val truth = "com.google.truth:truth:1.0"
            const val mockito = "org.mockito:mockito-core:3.1.0"
        }
    }
}
