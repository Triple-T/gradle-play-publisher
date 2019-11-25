package com.github.triplet.gradle.androidpublisher

import java.io.File

abstract class FakePlayPublisher : PlayPublisher {
    fun install() {
        publisher = this
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

    class Factory : PlayPublisher.Factory {
        override fun create(credentials: File, email: String?, appId: String) = publisher
    }

    companion object {
        lateinit var publisher: PlayPublisher
    }
}
