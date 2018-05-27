package de.triplet.gradle.play

import java.io.File

data class PlayAccountConfig @JvmOverloads constructor(
        internal val name: String = "", // Needed for Gradle

        var jsonFile: File? = null,
        var pk12File: File? = null,
        var serviceAccountEmail: String? = null
)
