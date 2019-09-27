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
