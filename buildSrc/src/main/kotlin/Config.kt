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
            const val agp = "com.android.tools.build:gradle:4.1.0-alpha02"
            const val ap =
                    "com.google.apis:google-api-services-androidpublisher:v3-rev20200126-1.30.8"
            const val googleClient = "com.google.api-client:google-api-client:1.30.9"
            const val guava = "com.google.guava:guava:28.2-jre"
            const val jackson = "com.google.http-client:google-http-client-jackson2:1.34.2"

            const val junit = "junit:junit:4.13"
            const val truth = "com.google.truth:truth:1.0.1"
            const val mockito = "org.mockito:mockito-core:3.3.3"
        }
    }
}
