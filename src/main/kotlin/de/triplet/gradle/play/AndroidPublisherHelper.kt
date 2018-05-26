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

import com.google.api.client.http.HttpRequestInitializer
import com.google.api.services.androidpublisher.AndroidPublisher

internal object AndroidPublisherHelper {
    fun init(extension: PlayPublisherPluginExtension, config: PlayAccountConfig?): AndroidPublisher {
        return AndroidPublisher.Builder(HTTP_TRANSPORT, JSON_FACTORY, HttpRequestInitializer {
            (config?.authorization
                    ?: extension.authorization
                    ?: throw IllegalArgumentException("No credentials provided.")).initialize(it.apply {
                connectTimeout = 100_000
                readTimeout = 100_000
            })
        })
                .setApplicationName(APPLICATION_NAME)
                .build()
    }
}