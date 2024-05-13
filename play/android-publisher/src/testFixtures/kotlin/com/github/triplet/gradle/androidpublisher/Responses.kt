package com.github.triplet.gradle.androidpublisher

import com.google.api.client.googleapis.testing.json.GoogleJsonResponseExceptionFactoryTesting
import com.google.api.client.json.gson.GsonFactory

fun newGppAppDetails(
        defaultLocale: String?,
        contactEmail: String?,
        contactPhone: String?,
        contactWebsite: String?,
) = GppAppDetails(defaultLocale, contactEmail, contactPhone, contactWebsite)

fun newGppListing(
        locale: String,
        fullDescription: String?,
        shortDescription: String?,
        title: String?,
        video: String?,
) = GppListing(locale, fullDescription, shortDescription, title, video)

fun newImage(url: String, sha256: String) = GppImage(url, sha256)

fun newReleaseNote(
        track: String,
        locale: String,
        contents: String,
) = ReleaseNote(track, locale, contents)

fun newSuccessEditResponse(id: String) = EditResponse.Success(id)

fun newFailureEditResponse(reason: String) = EditResponse.Failure(
        GoogleJsonResponseExceptionFactoryTesting.newMock(
                GsonFactory.getDefaultInstance(), 400, reason))

fun newSuccessCommitResponse() = CommitResponse.Success

fun newUploadInternalSharingArtifactResponse(json: String, downloadUrl: String) =
        UploadInternalSharingArtifactResponse(json, downloadUrl)

fun newGppProduct(sku: String, json: String) = GppProduct(sku, json)

fun newUpdateProductResponse(needsCreating: Boolean) = UpdateProductResponse(needsCreating)

fun newGppSubscription(productId: String, json: String) = GppSubscription(productId, json)

fun newUpdateSubscriptionResponse(needsCreating: Boolean) = UpdateSubscriptionResponse(needsCreating)
