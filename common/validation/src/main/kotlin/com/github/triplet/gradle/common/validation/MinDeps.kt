package com.github.triplet.gradle.common.validation

import com.android.build.api.AndroidPluginVersion
import org.gradle.util.GradleVersion

internal val MIN_GRADLE_VERSION = GradleVersion.version("9.1.0")
internal val MIN_AGP_VERSION = AndroidPluginVersion(9, 0)
