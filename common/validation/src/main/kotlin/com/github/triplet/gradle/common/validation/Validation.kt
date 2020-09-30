package com.github.triplet.gradle.common.validation

import com.android.build.api.component.analytics.AnalyticsEnabledApplicationVariant
import com.android.build.api.variant.ApplicationVariant
import com.android.build.gradle.internal.component.ApkCreationConfig
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.kotlin.dsl.apply

/**
 * Validates required dependencies. If GPP can't run in the current context, an error will be
 * thrown.
 */
fun Project.validateRuntime() {
    rootProject.plugins.apply(RuntimeValidationPlugin::class)
}

/** @return true if the variant is *not* debuggable and can therefore be published. */
fun validateDebuggability(variant: ApplicationVariant, logger: Logger): Boolean {
    val hackToGetDebuggable =
            ((variant as AnalyticsEnabledApplicationVariant).delegate as ApkCreationConfig)
    val isValid = !hackToGetDebuggable.debuggable

    if (!isValid) {
        val typeName = variant.buildType
        if (typeName.equals("release", true)) {
            logger.error("GPP cannot configure variant '${variant.name}' because it is debuggable")
        } else {
            logger.info("Skipping debuggable build with type '$typeName'")
        }
    }

    return isValid
}
