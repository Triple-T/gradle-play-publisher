/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.triplet.gradle.play

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import java.io.File

internal object AndroidPublisherHelper {
    fun init(extension: PlayPublisherPluginExtension, config: PlayAccountConfig?): AndroidPublisher {
        val credential = if (config?.jsonFile != null) {
            authorizeWithServiceAccount(config.jsonFile!!)
        } else if (config?.serviceAccountEmail != null && config.pk12File != null) {
            authorizeWithServiceAccount(config.serviceAccountEmail!!, config.pk12File!!)
        } else if (extension.jsonFile != null) {
            authorizeWithServiceAccount(extension.jsonFile!!)
        } else if (extension.serviceAccountEmail != null && extension.pk12File != null) {
            authorizeWithServiceAccount(extension.serviceAccountEmail!!, extension.pk12File!!)
        } else {
            throw IllegalArgumentException("No credentials provided.")
        }

        // Set up and return API client.
        return AndroidPublisher.Builder(HTTP_TRANSPORT, JSON_FACTORY, HttpRequestInitializer {
            it.apply {
                connectTimeout = extension.connectionTimeout
                readTimeout = extension.connectionTimeout
            }
            credential.initialize(it)
        })
                .setApplicationName(APPLICATION_NAME)
                .build()
    }

    private fun authorizeWithServiceAccount(serviceAccountEmail: String, pk12File: File): Credential {
        // Build service account credential.
        return GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(serviceAccountEmail)
                .setServiceAccountScopes(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
                .setServiceAccountPrivateKeyFromP12File(pk12File)
                .build()
    }

    private fun authorizeWithServiceAccount(jsonFile: File): Credential {
        val credential = GoogleCredential.fromStream(jsonFile.inputStream(), HTTP_TRANSPORT, JSON_FACTORY)
        return credential.createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
    }
}
