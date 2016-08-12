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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to initialize the publisher APIs client library.
 * <p>
 * Before making any calls to the API through the client library you need to
 * call the {@link AndroidPublisherHelper#init(String, File)} method. This will run
 * all precondition checks for client id and secret setup properly in
 * resources/client_secrets.json and authorize this client against the API.
 * </p>
 */
public class AndroidPublisherHelper {

    private static final Log log = LogFactory.getLog(AndroidPublisherHelper.class);

    private static final String APPLICATION_NAME = "gradle-play-publisher"

    static final String MIME_TYPE_IMAGE= "image/*";

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    private static Credential authorizeWithServiceAccount(PlayPublisherPluginExtension extension)
            throws GeneralSecurityException, IOException {
        if (extension.serviceAccountEmail && extension.pk12File) {
            return authorizeWithServiceAccount(extension.serviceAccountEmail, extension.pk12File);
        } else if (extension.jsonFile) {
            return authorizeWithServiceAccount(extension.jsonFile)
        }
        throw new IllegalArgumentException("No credentials provided.");
    }

    private static Credential authorizeWithServiceAccount(String serviceAccountEmail, File pk12File)
            throws GeneralSecurityException, IOException {
        log.info(String.format("Authorizing using Service Account: %s", serviceAccountEmail));

        // Build service account credential.
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(serviceAccountEmail)
                .setServiceAccountScopes(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER))
                .setServiceAccountPrivateKeyFromP12File(pk12File)
                .build();
        return credential;
    }

    private static Credential authorizeWithServiceAccount(File jsonFile) throws IOException {
        Path path = Paths.get(jsonFile.absolutePath);
        InputStream serviceAccountStream = new ByteArrayInputStream(Files.readAllBytes(path));
        GoogleCredential credential = GoogleCredential
                .fromStream(serviceAccountStream, HTTP_TRANSPORT, JSON_FACTORY);
        return credential.createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));
    }

    /**
     * Performs all necessary setup steps for running requests against the API.
     *
     * @param serviceAccountEmail the Service Account Email (empty if using
     *            installed application)
     * @return the {@Link AndroidPublisher} service
     * @throws GeneralSecurityException
     * @throws IOException
     */
    protected static AndroidPublisher init(PlayPublisherPluginExtension extension)
            throws IOException, GeneralSecurityException {

        // Authorization.
        newTrustedTransport();
        Credential credential = authorizeWithServiceAccount(extension);

        // Set up and return API client.
        return new AndroidPublisher.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static void newTrustedTransport() throws GeneralSecurityException,
            IOException {
        if (HTTP_TRANSPORT == null) {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        }
    }

}