package com.github.triplet.gradle.play.internal

import com.android.build.api.variant.ApplicationVariant
import com.github.triplet.gradle.common.validation.validateDebuggability
import com.github.triplet.gradle.play.PlayPublisherPlugin
import org.gradle.api.logging.Logging

internal fun ApplicationVariant.validateDebuggability() =
        validateDebuggability(this, Logging.getLogger(PlayPublisherPlugin::class.java))
