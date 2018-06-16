package com.github.triplet.gradle.play.lint

import java.nio.file.Path

internal operator fun Path.get(index: Int) = if (index < nameCount) getName(index).toString() else null
