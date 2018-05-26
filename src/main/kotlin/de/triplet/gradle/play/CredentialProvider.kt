package de.triplet.gradle.play

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import java.io.File

abstract class CredentialProvider {
    var serviceAccountEmail: String? = null

    var pk12File: File? = null

    var jsonFile: File? = null

    val authorization: Credential?
        get() {
            return authorizeWithServiceAccount(jsonFile)
                ?: authorizeWithServiceAccount(serviceAccountEmail, pk12File)
        }

    private fun authorizeWithServiceAccount(serviceAccountEmail: String?, pk12File: File?): Credential? {
        // Build service account credential.
        if (serviceAccountEmail == null || pk12File == null) return null
        return GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(serviceAccountEmail)
                .setServiceAccountScopes(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
                .setServiceAccountPrivateKeyFromP12File(pk12File)
                .build()
    }

    private fun authorizeWithServiceAccount(jsonFile: File?): Credential? {
        if (jsonFile == null) return null
        val credential = GoogleCredential.fromStream(jsonFile?.inputStream(), HTTP_TRANSPORT, JSON_FACTORY)
        return credential.createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
    }
}