package com.github.triplet.gradle.play.helpers

import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.UpdateProductResponse
import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import java.io.File

open class DefaultPlayPublisher : PlayPublisher {
    fun install() {
        PlayPublisher.setFactory(object : PlayPublisher.Factory {
            override fun create(credentials: File, email: String?, appId: String) =
                    this@DefaultPlayPublisher
        })
    }

    override fun uploadInternalSharingBundle(bundleFile: File): UploadInternalSharingArtifactResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun uploadInternalSharingApk(apkFile: File): UploadInternalSharingArtifactResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun insertInAppProduct(productFile: File): Unit =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun updateInAppProduct(productFile: File): UpdateProductResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")
}
