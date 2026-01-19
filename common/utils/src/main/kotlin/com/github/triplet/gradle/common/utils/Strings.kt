package com.github.triplet.gradle.common.utils

import java.util.Locale.getDefault

fun String.capitalize(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
