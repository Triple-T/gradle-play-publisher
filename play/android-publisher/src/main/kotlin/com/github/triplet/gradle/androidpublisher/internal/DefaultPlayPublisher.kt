package com.github.triplet.gradle.androidpublisher.internal

import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.AndroidPublisher
import java.io.File

internal class DefaultPlayPublisher(
        private val publisher: AndroidPublisher,
        private val appId: String
) : PlayPublisher {
    override fun uploadInternalSharingBundle(bundleFile: File): String {
        val bundle = publisher.internalappsharingartifacts()
                .uploadbundle(appId, FileContent(MIME_TYPE_STREAM, bundleFile))
                .trackUploadProgress("App Bundle", bundleFile)
                .execute()

        println("Upload successful: ${bundle.downloadUrl}")

        return bundle.toPrettyString()
    }

    companion object : PlayPublisher.Factory {
        override fun create(
                credentials: File,
                email: String?,
                appId: String
        ): PlayPublisher {
            val publisher = createPublisher(ServiceAccountAuth(credentials, email))
            return DefaultPlayPublisher(publisher, appId)
        }
    }
}
