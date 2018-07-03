package com.github.triplet.gradle.play.internal

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.ProductFlavor
import com.android.builder.model.Version
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.util.GradleVersion

private val MIN_GRADLE_VERSION: GradleVersion = GradleVersion.version("4.1")
private const val MIN_AGP_VERSION: String = "3.0.1"

internal val BaseVariant.flavorNameOrDefault get() = flavorName.nullOrFull() ?: "main"

internal val BaseVariant.playPath get() = "$RESOURCES_OUTPUT_PATH/$name"

private val ProductFlavor.extras
    get() = requireNotNull((this as ExtensionAware).extensions.get<ExtraPropertiesExtension>())

internal inline fun <reified T> ExtensionContainer.get() = findByType(T::class.java)

internal inline fun <reified T : Task> Project.newTask(
        name: String,
        description: String,
        group: String? = PLUGIN_GROUP,
        block: T.() -> Unit = {}
): T = tasks.create(name, T::class.java).apply {
    this.description = description
    this.group = group
    block()
}

internal operator fun ProductFlavor.get(name: String) = extras[name]

internal operator fun ProductFlavor.set(name: String, value: Any?) {
    extras[name] = value
}

internal fun validateRuntime() {
    val gradleVersion = GradleVersion.current()
    check(gradleVersion >= MIN_GRADLE_VERSION) {
        "Gradle Play Publisher's minimum Gradle version is at least $MIN_GRADLE_VERSION and " +
                "yours is $gradleVersion. Find the latest version at " +
                "https://github.com/gradle/gradle/releases, then run " +
                "'./gradlew wrapper --gradle-version=\$LATEST --distribution-type=ALL'."
    }

    val agpVersion = Version.ANDROID_GRADLE_PLUGIN_VERSION
    check(agpVersion >= MIN_AGP_VERSION) {
        "Gradle Play Publisher's minimum Android Gradle Plugin version is at least " +
                "$MIN_AGP_VERSION and yours is $agpVersion. Find the latest version and upgrade " +
                "instructions at https://developer.android.com/studio/releases/gradle-plugin."
    }
}
