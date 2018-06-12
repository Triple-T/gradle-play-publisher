package com.github.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.github.triplet.gradle.play.internal.AccountConfig
import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.TrackType
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class PlayPublisherExtension : AccountConfig by PlayAccountConfigExtension() {
    @get:Internal("Backing property for public input")
    internal var _track = TrackType.INTERNAL
    /**
     * Specify the track in which to upload your app. May be one of internal, alpha, beta, rollout,
     * or production. Default is internal.
     */
    @get:Input
    var track
        get() = _track.publishedName
        set(value) {
            _track = requireNotNull(TrackType.values().find { it.publishedName == value }) {
                "Track must be one of ${TrackType.values().joinToString { "'${it.publishedName}'" }}"
            }
        }
    /**
     * Choose whether or not to untrack superseded versions automatically. See
     * https://github.com/Triple-T/gradle-play-publisher#untrack-conflicting-versions. Disabled by
     * default.
     */
    @get:Input
    var untrackOld = false
    /**
     * Specify the initial user percent intended to receive a 'rollout' update (see [track]).
     * Default is 10% == 0.1.
     */
    @get:Input
    var userFraction = 0.1
    /**
     * Choose whether or not to throw an error should a Play Store listing detail be too large or
     * simply trim it. Default throws.
     */
    @get:Input
    var errorOnSizeLimit = true

    @get:Internal("Backing property for public input")
    internal var _resolutionStrategy = ResolutionStrategy.FAIL
    /**
     * Specify the resolution strategy to employ when a version conflict occurs. May be one of auto,
     * fail, or ignore. Default is ignore.
     */
    @get:Input
    var resolutionStrategy
        get() = _resolutionStrategy.publishedName
        set(value) {
            _resolutionStrategy = requireNotNull(
                    ResolutionStrategy.values().find { it.publishedName == value }
            ) {
                "Resolution strategy must be one of " +
                        ResolutionStrategy.values().joinToString { "'${it.publishedName}'" }
            }
        }
    /**
     * If the [resolutionStrategy] is auto, process the outputs such that they will pass validation
     * when uploaded.
     */
    @get:Internal("ProcessPackageMetadataTask is always out-of-date. Also, Closures with " +
                          "parameters cannot be used as inputs.")
    var autoResolutionHandler: (inputs: AutoResolutionInputs) -> Unit =
            AutoResolutionInputs::runDefault

    @get:Internal("Backing property for public input")
    internal lateinit var _releaseStatus: ReleaseStatus
    /**
     * Specify the status to apply to the uploaded app release. May be one of completed, draft,
     * halted, or inProgress. Default is completed for all tracks except rollout where inProgress is
     * the default.
     */
    @get:Input
    var releaseStatus
        get() = when {
            ::_releaseStatus.isInitialized -> _releaseStatus
            _track == TrackType.ROLLOUT -> ReleaseStatus.IN_PROGRESS
            else -> ReleaseStatus.COMPLETED
        }.publishedName
        set(value) {
            _releaseStatus = requireNotNull(
                    ReleaseStatus.values().find { it.publishedName == value }
            ) {
                "Release Status must be one of " +
                        ReleaseStatus.values().joinToString { "'${it.publishedName}'" }
            }
        }
}

data class AutoResolutionInputs(
        /** The outputs to be processed. */
        val outputs: List<ApkVariantOutput>,
        /** The highest version code uploaded to the Play Store. */
        val highestRemoteVersionCode: Long
) {
    /**
     * Linearly shifts all version codes such that the smallest one is 1 unit greater than the
     * maximum version code found in the Play Store.
     *
     * This logic is provided in standalone form to support merging your own custom handler logic on
     * top of the default. For example, you could run this handler and then update each output's
     * version name using the newly mutated version codes.
     *
     * Note: applying this handler permanently mutates the outputs.
     *
     * @see [PlayPublisherExtension.autoResolutionHandler]
     */
    fun runDefault() {
        val smallestVersionCode = outputs.map { it.versionCode }.min() ?: 1

        val patch = highestRemoteVersionCode - smallestVersionCode + 1
        if (patch <= 0) return // Nothing to do, outputs are already greater than remote

        for (output in outputs) output.versionCodeOverride = output.versionCode + patch.toInt()
    }
}
