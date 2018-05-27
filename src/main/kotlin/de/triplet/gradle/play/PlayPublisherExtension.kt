package de.triplet.gradle.play

import de.triplet.gradle.play.internal.Alias
import de.triplet.gradle.play.internal.Track
import de.triplet.gradle.play.internal.publishedName

open class PlayPublisherExtension {
    internal val accountConfig = PlayAccountConfig()
    var jsonFile by Alias(accountConfig::jsonFile)
    var pk12File by Alias(accountConfig::pk12File)
    var serviceAccountEmail by Alias(accountConfig::serviceAccountEmail)

    internal var _track = Track.INTERNAL
    var track
        get() = _track.publishedName
        set(value) {
            requireNotNull(Track.values().find { it.name.equals(value, true) }) {
                "Track must be one of ${Track.values().joinToString { "'${it.publishedName}'" }}"
            }
            _track = Track.valueOf(value.toUpperCase())
        }
    var untrackOld = false
    var userFraction = 0.1

    var uploadImages = false
    var errorOnSizeLimit = true
}
