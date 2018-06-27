package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.AccountConfig
import org.gradle.api.tasks.*
import java.io.File

data class PlayAccountConfigExtension @JvmOverloads constructor(
        @get:Internal internal val name: String = "", // Needed for Gradle

        @get:PathSensitive(PathSensitivity.RELATIVE) @get:InputFile @get:Optional
        override var serviceAccountCredentials: File? = null,
        @get:Input @get:Optional
        override var serviceAccountEmail: String? = null
) : AccountConfig
