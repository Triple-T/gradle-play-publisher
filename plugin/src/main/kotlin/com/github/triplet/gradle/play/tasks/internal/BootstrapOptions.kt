package com.github.triplet.gradle.play.tasks.internal

import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import java.util.concurrent.atomic.AtomicBoolean

internal interface BootstrapOptions {
    @get:Internal
    @set:Option(
            option = "app-details",
            description = "Download app details such as your contact email."
    )
    var downloadAppDetails: Boolean

    @get:Internal
    @set:Option(
            option = "listings",
            description = "Download listings for each language such as your app title and graphics."
    )
    var downloadListings: Boolean

    @get:Internal
    @set:Option(option = "release-notes", description = "Download release notes for each language.")
    var downloadReleaseNotes: Boolean

    @get:Internal
    @set:Option(option = "products", description = "Download in-app purchases and subscriptions.")
    var downloadProducts: Boolean
}

internal object BootstrapOptionsHolder : BootstrapOptions {
    private val isRequestingSpecificFeature = AtomicBoolean()

    override var downloadAppDetails = true
        set(value) {
            onSet()
            field = value
        }
    override var downloadListings = true
        set(value) {
            onSet()
            field = value
        }
    override var downloadReleaseNotes = true
        set(value) {
            onSet()
            field = value
        }
    override var downloadProducts = true
        set(value) {
            onSet()
            field = value
        }

    /**
     * Since this is an object, it won't be destroyed across Gradle invocations. Therefore, we need
     * to manually reset it every time our plugin is applied.
     */
    fun reset() {
        downloadAppDetails = true
        downloadListings = true
        downloadReleaseNotes = true
        downloadProducts = true

        // Must come after to override onSet
        isRequestingSpecificFeature.set(false)
    }

    /**
     * By default, we download all features. However, if they are specified with CLI options, we
     * only download those features.
     *
     * Note: this method must be called before updating the field since it may overwrite them.
     */
    private fun onSet() {
        if (isRequestingSpecificFeature.compareAndSet(false, true)) {
            downloadAppDetails = false
            downloadListings = false
            downloadReleaseNotes = false
            downloadProducts = false
        }
    }
}

internal open class BootstrapLifecycleHelperTask : LifecycleHelperTask(),
        BootstrapOptions by BootstrapOptionsHolder
