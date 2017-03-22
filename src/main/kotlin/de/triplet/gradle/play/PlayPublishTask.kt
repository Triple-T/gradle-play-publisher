package de.triplet.gradle.play

import com.android.build.gradle.api.ApplicationVariant
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.androidpublisher.AndroidPublisher
import org.gradle.api.DefaultTask

open class PlayPublishTask : DefaultTask() {

    lateinit var extension: PlayPublisherPluginExtension

    lateinit var variant: ApplicationVariant

    lateinit var edits: AndroidPublisher.Edits

    lateinit var editId: String

    var playAccountConfig: PlayAccountConfig? = null

    var service: AndroidPublisher? = null

    fun publish() {
        if (service == null) {
            service = AndroidPublisherHelper.init(extension, playAccountConfig)
        }

        edits = service!!.edits()

        // Create a new edit to make changes to your listing.
        val editRequest = edits.insert(variant.applicationId, null /* no content yet */)

        try {
            val edit = editRequest.execute()
            editId = edit.id
        } catch (e: GoogleJsonResponseException) {
            // The very first release has to be uploaded via the web interface.
            // We add a little explanation to Google's exception.
            if (e.message?.contains("applicationNotFound") == true) {
                throw IllegalArgumentException("No application was found for the package name ${variant.applicationId}. Is this the first release for this app? The first version has to be uploaded via the web interface.", e)
            }

            // Just rethrow everything else.
            throw e
        }
    }
}
