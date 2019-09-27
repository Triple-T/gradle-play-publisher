package com.github.triplet.gradle.play.internal.validation

import com.android.Version
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.PlayPublisherPlugin
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import org.gradle.api.logging.Logging
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

private val MIN_GRADLE_VERSION = GradleVersion.version("5.6.1")
private val MIN_AGP_VERSION = VersionNumber.parse("3.5.0")

internal fun validateRuntime() {
    val agpVersion = VersionNumber.parse(try {
        Version.ANDROID_GRADLE_PLUGIN_VERSION
    } catch (e: NoClassDefFoundError) {
        @Suppress("DEPRECATION") // TODO remove when 3.6 is the minimum
        com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
    })
    val validator = RuntimeValidator(
            GradleVersion.current(), MIN_GRADLE_VERSION, agpVersion, MIN_AGP_VERSION)

    validator.validate()
}

internal fun PlayPublisherExtension.validateCreds() {
    val creds = checkNotNull(config.serviceAccountCredentials) {
        "No credentials specified. Please read our docs for more details: " +
                "https://github.com/Triple-T/gradle-play-publisher" +
                "#authenticating-gradle-play-publisher"
    }

    if (creds.extension.equals("json", true)) {
        check(config.serviceAccountEmail == null) {
            "JSON credentials cannot specify a service account email."
        }
    } else {
        check(config.serviceAccountEmail != null) {
            "PKCS12 credentials must specify a service account email."
        }
    }
}

internal fun ApplicationVariant.validateDebuggability(): Boolean =
        validateDebuggabilityInternal(this)

private fun validateDebuggabilityInternal(variant: ApplicationVariant): Boolean {
    val isValid = !variant.buildType.isDebuggable

    if (!isValid) {
        val logger = Logging.getLogger(PlayPublisherPlugin::class.java)
        val typeName = variant.buildType.name
        if (typeName.equals("release", true)) {
            Logging.getLogger(PlayPublisherPlugin::class.java).error(
                    "GPP cannot configure variant '${variant.name}' because it is debuggable")
        } else {
            logger.info("Skipping debuggable build with type '$typeName'")
        }
    }

    return isValid
}

internal infix fun GoogleJsonResponseException.has(error: String) =
        details?.errors.orEmpty().any { it.reason == error }
