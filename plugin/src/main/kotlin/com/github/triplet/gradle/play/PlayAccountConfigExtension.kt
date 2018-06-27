package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.AccountConfig
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

data class PlayAccountConfigExtension @JvmOverloads constructor(
        @get:Internal internal val name: String = "", // Needed for Gradle

        @get:PathSensitive(PathSensitivity.RELATIVE) @get:Optional @get:InputFile
        override var serviceAccountCredentials: File? = null,
        @get:Optional @get:Input
        override var serviceAccountEmail: String? = null
) : AccountConfig
