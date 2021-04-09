package com.github.triplet.gradle.play.internal

import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.ApplicationVariantProperties
import com.github.triplet.gradle.common.validation.validateDebuggability
import com.github.triplet.gradle.play.PlayPublisherPlugin
import org.gradle.api.logging.Logging

internal fun ApplicationVariant<ApplicationVariantProperties>.validateDebuggability() =
        validateDebuggability(this, Logging.getLogger(PlayPublisherPlugin::class.java))
