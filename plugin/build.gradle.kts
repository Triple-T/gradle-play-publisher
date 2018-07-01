import org.codehaus.groovy.runtime.InvokerHelper

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("groovy")
    id("java-gradle-plugin")
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation("com.android.tools.build:gradle:3.0.1")
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev12-1.23.0") {
        exclude("com.google.guava", "guava-jdk5") // Remove when upgrading to AGP 3.1+
    }
    implementation(kotlin("stdlib-jdk7"))

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.18.3")
    testImplementation("org.assertj:assertj-core:3.10.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

gradlePlugin {
    plugins {
        create("play") {
            id = "com.github.triplet.play"
            implementationClass = "com.github.triplet.gradle.play.PlayPublisherPlugin"
        }
    }
}

afterEvaluate {
    fun PomFilterContainer.removeTestDependencies() {
        pom.whenConfigured {
            dependencies.removeIf {
                // Stolen from JetBrains' own sample at
                // https://github.com/JetBrains/kotlin/blob/v1.2.50/buildSrc/src/main/kotlin/plugins/PublishedKotlinModule.kt#L86
                InvokerHelper.getMetaClass(it).getProperty(it, "scope") == "test"
            }
        }
    }

    tasks.withType<Upload> {
        repositories.forEach {
            (it as? PomFilterContainer)?.removeTestDependencies()
        }
    }
}

tasks.withType<Test> {
    // Our integration tests need a fully compiled jar
    dependsOn("assemble")

    // Those tests also need to know which version code was built
    project.property("VERSION_NAME")?.let {
        systemProperty("VERSION_NAME", it)
    }
}
