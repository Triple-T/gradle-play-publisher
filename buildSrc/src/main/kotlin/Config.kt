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
            const val agp = "com.android.tools.build:gradle:4.0.0-alpha09"
            const val ap =
                    "com.google.apis:google-api-services-androidpublisher:v3-rev20191202-1.30.3"
            const val googleClient = "com.google.api-client:google-api-client:1.30.7"
            const val guava = "com.google.guava:guava:28.2-jre"
            const val jackson = "com.google.http-client:google-http-client-jackson2:1.34.2"

            const val junit = "junit:junit:4.13"
            const val truth = "com.google.truth:truth:1.0.1"
            const val mockito = "org.mockito:mockito-core:3.2.4"
        }
    }
}
