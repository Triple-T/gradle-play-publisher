package de.triplet.gradle.play

open class PlayPublisherPluginExtension : CredentialProvider() {
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
}
