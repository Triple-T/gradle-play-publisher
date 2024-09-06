package com.github.triplet.gradle.common.validation

import com.android.build.api.component.analytics.AnalyticsEnabledApplicationVariant
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.component.ComponentCreationConfig
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion

/**
 * Validates required dependencies. If GPP can't run in the current context, an error will be
 * thrown.
 */
fun Project.validateRuntime() {
    GradleRuntimeValidator(GradleVersion.current(), MIN_GRADLE_VERSION).validate()
    plugins.withType<AppPlugin> {
        val agpPluginVersion = extensions.findByType<ApplicationAndroidComponentsExtension>()?.pluginVersion
        AgpRuntimeValidator(agpPluginVersion, MIN_AGP_VERSION).validate()
    }
}

/** @return true if the variant is *not* debuggable and can therefore be published. */
fun validateDebuggability(variant: ApplicationVariant, logger: Logger): Boolean {
    val hackToGetDebuggable =
            ((variant as? AnalyticsEnabledApplicationVariant)?.delegate ?: variant)
                    as ComponentCreationConfig
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
