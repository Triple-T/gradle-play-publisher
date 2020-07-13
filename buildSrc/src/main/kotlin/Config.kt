import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.publish.maven.MavenPublication

val Project.isSnapshotBuild
    get() = (version as String).contains("snapshot", true)

fun RepositoryHandler.deps() {
    google().content {
        includeGroup("com.android")
        includeGroupByRegex("com\\.android\\..*")
        includeGroupByRegex("com\\.google\\..*")
        includeGroupByRegex("androidx\\..*")
    }

    jcenter()
}

fun MavenPublication.configurePom() = pom {
    name.set("Google Play Publisher")
    description.set("Gradle Play Publisher is a plugin that allows you to upload your " +
                            "App Bundle or APK and other app details to the " +
                            "Google Play Store.")
    url.set("https://github.com/Triple-T/gradle-play-publisher")

    licenses {
        license {
            name.set("The MIT License (MIT)")
            url.set("http://opensource.org/licenses/MIT")
            distribution.set("repo")
        }
    }

    developers {
        developer {
            id.set("SUPERCILEX")
            name.set("Alex Saveau")
            email.set("saveau.alexandre@gmail.com")
            roles.set(listOf("Owner"))
            timezone.set("-8")
        }
    }

    scm {
        connection.set("scm:git@github.com:Triple-T/gradle-play-publisher.git")
        developerConnection.set("scm:git@github.com:Triple-T/gradle-play-publisher.git")
        url.set("https://github.com/Triple-T/gradle-play-publisher")
    }
}

fun Project.configureMaven(handler: RepositoryHandler) = handler.maven {
    name = if (isSnapshotBuild) "Snapshots" else "Release"
    url = if (isSnapshotBuild) {
        uri("https://oss.sonatype.org/content/repositories/snapshots/")
    } else {
        uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
    }

    credentials {
        username = System.getenv("SONATYPE_NEXUS_USERNAME")
        password = System.getenv("SONATYPE_NEXUS_PASSWORD")
    }
}

object Config {
    object Libs {
        object All {
            const val agp = "com.android.tools.build:gradle:4.2.0-alpha01"
            const val ap =
                    "com.google.apis:google-api-services-androidpublisher:v3-rev20200526-1.30.9"
            const val googleClient = "com.google.api-client:google-api-client:1.30.9"
            const val apacheClientV2 = "com.google.http-client:google-http-client-apache-v2:1.35.0"
            const val auth = "com.google.auth:google-auth-library-oauth2-http:0.20.0"
            const val guava = "com.google.guava:guava:29.0-jre"
            const val jackson = "com.google.http-client:google-http-client-jackson2:1.35.0"

            const val junit = "org.junit.jupiter:junit-jupiter-api:5.6.2"
            const val junitEngine = "org.junit.jupiter:junit-jupiter-engine:5.6.2"
            const val truth = "com.google.truth:truth:1.0.1"
            const val mockito = "org.mockito:mockito-core:3.3.3"
        }
    }
}
