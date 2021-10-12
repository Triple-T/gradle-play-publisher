import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.publish.maven.MavenPublication

val Project.isSnapshotBuild
    get() = (version as String).contains("snapshot", true)

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
