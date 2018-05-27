package de.triplet.gradle.play

import de.triplet.gradle.play.internal.Track
import de.triplet.gradle.play.internal.publishedName

open class PlayPublisherPluginExtension {
    var jsonFile
        get() = accountConfig.jsonFile
        set(value) {
            accountConfig.jsonFile = value
        }
    var pk12File
        get() = accountConfig.pk12File
        set(value) {
            accountConfig.pk12File = value
        }
    var serviceAccountEmail
        get() = accountConfig.serviceAccountEmail
        set(value) {
            accountConfig.serviceAccountEmail = value
        }

    internal var _track = Track.INTERNAL
    var track
        get() = _track.publishedName
        set(value) {
            requireNotNull(Track.values().find { it.name.equals(value, true) }) {
                "Track has to be one of ${Track.values().joinToString { "'${it.publishedName}'" }}"
            }
            _track = Track.valueOf(value.toUpperCase())
        }
    var untrackOld = false
    var userFraction = 0.1

    var uploadImages = false
    var errorOnSizeLimit = true

    internal val accountConfig = PlayAccountConfig()
}
