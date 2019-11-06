package com.github.triplet.gradle.play.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.common.validation.validateDebuggability
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.PlayPublisherPlugin
import org.gradle.api.logging.Logging

internal fun PlayPublisherExtension.validateCreds() {
    val creds = checkNotNull(config.serviceAccountCredentials) {
        "No credentials specified. Please read our docs for more details: " +
                "https://github.com/Triple-T/gradle-play-publisher" +
                "#authenticating-gradle-play-publisher"
    }

    // TODO(#710): remove once support for PKCS12 creds is gone
    if (creds.extension.equals("json", true)) {
        check(config.serviceAccountEmail == null) {
            "JSON credentials cannot specify a service account email."
        }
    } else {
        check(config.serviceAccountEmail != null) {
            "PKCS12 credentials must specify a service account email."
        }
        Logging.getLogger(PlayPublisherPlugin::class.java)
                .warn("PKCS12 based authentication is deprecated. " +
                              "Please use JSON credentials instead.")
    }
}

internal fun ApplicationVariant.validateDebuggability() =
        validateDebuggability(this, Logging.getLogger(PlayPublisherPlugin::class.java))
