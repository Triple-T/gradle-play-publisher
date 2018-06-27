package com.github.triplet.gradle.play.internal

import org.gradle.api.tasks.*
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
    @get:InputFile
    @get:Optional
    var serviceAccountCredentials: File?

    /** Service Account email. Only needed if PKCS12 credentials are used. */
    @get:Input
    @get:Optional
    var serviceAccountEmail: String?
}
