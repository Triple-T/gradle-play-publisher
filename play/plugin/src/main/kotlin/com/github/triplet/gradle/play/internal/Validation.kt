package com.github.triplet.gradle.play.internal

import com.android.build.api.variant.ApplicationVariant
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.common.validation.validateDebuggability
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.PlayPublisherPlugin
import org.gradle.api.logging.Logging

internal fun PlayPublisherExtension.validateCreds() {
    if (System.getenv(PlayPublisher.CREDENTIAL_ENV_VAR) != null) {
        return
    }

    check(serviceAccountCredentials.isPresent) {
        """
        |No credentials specified. Please read our docs for more details:
        |https://github.com/Triple-T/gradle-play-publisher#authenticating-gradle-play-publisher
        """.trimMargin()
    }
}

internal fun ApplicationVariant.validateDebuggability() =
        validateDebuggability(this, Logging.getLogger(PlayPublisherPlugin::class.java))
