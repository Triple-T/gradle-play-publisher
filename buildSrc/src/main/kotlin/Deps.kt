import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

fun RepositoryHandler.deps() {
    google().content {
        includeGroupByRegex("com\\.android\\..*")
        includeGroupByRegex("com\\.google\\..*")
        includeGroupByRegex("androidx\\..*")

        includeGroup("com.android")
        includeGroup("com.crashlytics.sdk.android")
        includeGroup("io.fabric.sdk.android")
    }

    maven("https://dl.bintray.com/kotlin/kotlin-dev/").content {
        includeGroup("org.jetbrains.kotlin")
    }

    jcenter()
}

object Config {
    object Libs {
        object All {
            const val agp = "com.android.tools.build:gradle:3.6.0-alpha11"
            const val ap =
                    "com.google.apis:google-api-services-androidpublisher:v3-rev20190818-1.30.1"

            const val truth = "com.google.truth:truth:1.0"
            const val junit = "junit:junit:4.12"
        }
    }
}
