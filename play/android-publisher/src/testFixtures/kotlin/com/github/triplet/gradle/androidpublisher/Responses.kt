package com.github.triplet.gradle.androidpublisher

fun newSuccessEditResponse(id: String) = EditResponse.Success(id)

fun newUploadInternalSharingArtifactResponse(json: String, downloadUrl: String) =
        UploadInternalSharingArtifactResponse(json, downloadUrl)

fun newUpdateProductResponse(needsCreating: Boolean) = UpdateProductResponse(needsCreating)
