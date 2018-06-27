package com.github.triplet.gradle.play.internal

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

interface AccountConfig {
    @get:Internal
    val _serviceAccountCredentials
        get() = checkNotNull(serviceAccountCredentials) { "No credentials provided" }
    /**
     * Service Account authentication file. Json is preferred, but PKCS12 is also supported. For
     * PKCS12 to work, the [serviceAccountEmail] must be specified.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional // Optional because it could be set in the `playAccountConfigs`
    @get:InputFile
    var serviceAccountCredentials: File?

    /** Service Account email. Only needed if PKCS12 credentials are used. */
    @get:Optional
    @get:Input
    var serviceAccountEmail: String?
}
