package com.github.triplet.gradle.play.helpers

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.PlayPublisher
import com.github.triplet.gradle.androidpublisher.UpdateProductResponse
import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import com.github.triplet.gradle.androidpublisher.installPlayPublisherFactory
import java.io.File

abstract class FakePlayPublisher : PlayPublisher {
    fun install() {
        installPlayPublisherFactory(object : PlayPublisher.Factory {
            override fun create(credentials: File, email: String?, appId: String) =
                    this@FakePlayPublisher
        })
    }

    override fun insertEdit(): EditResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun getEdit(id: String): EditResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun commitEdit(id: String): Unit =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun uploadInternalSharingBundle(bundleFile: File): UploadInternalSharingArtifactResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun uploadInternalSharingApk(apkFile: File): UploadInternalSharingArtifactResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun insertInAppProduct(productFile: File): Unit =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun updateInAppProduct(productFile: File): UpdateProductResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")
}
