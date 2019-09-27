repositories {
    jcenter()
}

plugins {
    `kotlin-dsl`
}

tasks.withType<ValidateTaskProperties>().configureEach {
    enableStricterValidation = true
    failOnWarning = true
}

dependencies {
    implementation("org.ajoberstar.grgit:grgit-gradle:3.1.1")
}
