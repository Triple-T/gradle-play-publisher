plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.gradle.java-library")
}

val androidToolsVersion = "26.1.1"

dependencies {
    //implementation(kotlin("stdlib-jdk7"))
    compileOnly("com.android.tools.lint:lint:$androidToolsVersion")
    compileOnly("com.android.tools.lint:lint-api:$androidToolsVersion")

    testImplementation("com.android.tools.lint:lint-checks:$androidToolsVersion")
    testImplementation("com.android.tools.lint:lint-tests:$androidToolsVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

val jar: Jar by tasks
jar.apply {
    manifest {
        attributes["Lint-Registry-v2"] = "com.github.triplet.gradle.play.lint.PlayPublisherIssueRegistry"
    }
}
