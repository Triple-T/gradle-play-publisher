package com.github.triplet.gradle.androidpublisher

import java.io.File
import java.io.InputStream

abstract class FakePlayPublisher : PlayPublisher {
    fun install() {
        publisher = this
    }

    override fun insertEdit(): EditResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun getEdit(id: String): EditResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun commitEdit(id: String, sendChangesForReview: Boolean): CommitResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun validateEdit(id: String): Unit =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun uploadInternalSharingBundle(bundleFile: File): UploadInternalSharingArtifactResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun uploadInternalSharingApk(apkFile: File): UploadInternalSharingArtifactResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun getInAppProducts(): List<GppProduct> =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun insertInAppProduct(productFile: File): Unit =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun updateInAppProduct(productFile: File): UpdateProductResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun getInAppSubscriptions(): List<GppSubscription> =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun insertInAppSubscription(subscriptionFile: File, regionsVersion: String): Unit =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    override fun updateInAppSubscription(subscriptionFile: File, regionsVersion: String): UpdateSubscriptionResponse =
            throw IllegalStateException("Test wasn't expecting this method to be called.")

    class Factory : PlayPublisher.Factory {
        override fun create(credentials: InputStream, appId: String) = publisher
        override fun create(appId: String, impersonateServiceAccount: String?) = publisher
    }

    companion object {
        lateinit var publisher: PlayPublisher
    }
}
