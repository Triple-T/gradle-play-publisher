package de.triplet.gradle.play

import de.triplet.gradle.play.internal.AccountConfig
import java.io.File

data class PlayAccountConfigExtension @JvmOverloads constructor(
        internal val name: String = "", // Needed for Gradle

        override var jsonFile: File? = null,
        override var pk12File: File? = null,
        override var serviceAccountEmail: String? = null
) : AccountConfig
