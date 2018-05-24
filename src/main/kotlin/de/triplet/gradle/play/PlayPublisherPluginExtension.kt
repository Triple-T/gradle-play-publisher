package de.triplet.gradle.play

import java.io.File

open class PlayPublisherPluginExtension {
    var serviceAccountEmail: String? = null

    var pk12File: File? = null

    var jsonFile: File? = null

    var uploadImages = false

    var errorOnSizeLimit = true

    var track = "alpha"
        set(value) {
            if (!(TRACKS.contains(value)))
                throw IllegalArgumentException("Track has to be one of 'internal', 'beta', 'rollout' or 'production")

            field = value
        }

    var untrackOld = false

    var userFraction = 0.1

    var connectionTimeout = 100_000
}
