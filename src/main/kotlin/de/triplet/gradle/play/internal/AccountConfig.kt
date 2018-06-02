package de.triplet.gradle.play.internal

import java.io.File

interface AccountConfig {
    /** Service Account authentication file. JSON will be prioitized over pk12. */
    var jsonFile: File?

    /** Service Account authentication file. JSON will be prioitized over pk12. */
    var pk12File: File?
    /** Service Account email. Only needed when using pk12 auth. */
    var serviceAccountEmail: String?
}
