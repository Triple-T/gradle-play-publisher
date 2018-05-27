package de.triplet.gradle.play

import de.triplet.gradle.play.internal.AccountConfig
import de.triplet.gradle.play.internal.Alias
import de.triplet.gradle.play.internal.Track

open class PlayPublisherExtension : AccountConfig {
    internal val accountConfig = PlayAccountConfigExtension()
    override var jsonFile by Alias(accountConfig::jsonFile)
    override var pk12File by Alias(accountConfig::pk12File)
    override var serviceAccountEmail by Alias(accountConfig::serviceAccountEmail)

    internal var _track = Track.INTERNAL
    /**
     * Specify the track in which to upload your app. May be one of internal, alpha, beta, rollout,
     * or production. Default is internal.
     */
    var track
        get() = _track.publishedName
        set(value) {
            _track = requireNotNull(Track.values().find { it.name.equals(value, true) }) {
                "Track must be one of ${Track.values().joinToString { "'${it.publishedName}'" }}"
            }
        }
    /**
     * Choose whether or not to untrack superseded versions automatically. See
     * https://github.com/Triple-T/gradle-play-publisher#untrack-conflicting-versions. Disabled by
     * default.
     */
    var untrackOld = false
    /**
     * Specify the initial user percent intended to receive a 'rollout' update (see [track]).
     * Default is 10% == 0.1.
     */
    var userFraction = 0.1

    /**
     * Choose whether or not to upload images when publishing the Play Store listing. Disabled by
     * default for performance reasons.
     */
    var uploadImages = false
    /**
     * Choose whether or not to throw an error should a Play Store listing detail be too large or
     * simply trim it. Default throws.
     */
    var errorOnSizeLimit = true
}
