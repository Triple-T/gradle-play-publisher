package com.github.triplet.gradle.play.internal

import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

interface AccountConfig {
    /** Service Account authentication file. JSON will be prioitized over pk12. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    @get:Optional
    var jsonFile: File?

    /** Service Account authentication file. JSON will be prioitized over pk12. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    @get:Optional
    var pk12File: File?
    /** Service Account email. Only needed when using pk12 auth. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    @get:Optional
    var serviceAccountEmail: String?
}
