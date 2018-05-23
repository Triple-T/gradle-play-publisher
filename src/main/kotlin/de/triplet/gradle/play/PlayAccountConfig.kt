package de.triplet.gradle.play

import java.io.File

open class PlayAccountConfig(var name: String = String()) {
    var serviceAccountEmail: String? = null

    var pk12File: File? = null

    var jsonFile: File? = null
}
