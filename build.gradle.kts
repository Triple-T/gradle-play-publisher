import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

buildscript {
    repositories.deps()

    dependencies {
        classpath(kotlin("gradle-plugin", embeddedKotlinVersion))
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.22.0"
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register<Delete>("clean") {
    delete("build")
}

tasks.register("ciBuild") {
    val isMaster = System.getenv("CIRCLE_BRANCH") == "master"
    val isPr = System.getenv("CIRCLE_PULL_REQUEST") != null
    val isSnapshot = project("plugin").version.toString().contains("snapshot", true)

    fun allTasks(name: String) = allprojects.mapNotNull { it.tasks.findByName(name) }
    if (isMaster && !isPr) { // Release build
        if (isSnapshot) {
            dependsOn(allTasks("build"), allTasks("publish"))
        } else {
            dependsOn(allTasks("build"))
        }
    } else {
        dependsOn(allTasks("check"))
    }
}

allprojects {
    repositories.deps()

    afterEvaluate {
        convention.findByType<KotlinProjectExtension>()?.apply {
            sourceSets.configureEach {
                languageSettings.progressiveMode = true
                languageSettings.enableLanguageFeature("NewInference")
                languageSettings.useExperimentalAnnotation(
                        "kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
    }

    if (System.getenv("CI") != null) {
        tasks.withType<Test> {
            testLogging {
                events("passed", "failed", "skipped")
                showStandardStreams = true
            }
        }
    }
}
