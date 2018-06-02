package de.triplet.gradle.play.internal

import com.android.build.gradle.api.ApplicationVariant
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import de.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.DefaultTask
import java.io.File

abstract class PlayPublishTaskBase : DefaultTask() {
    lateinit var extension: PlayPublisherExtension
    lateinit var variant: ApplicationVariant
    lateinit var accountConfig: AccountConfig

    private val publisher by lazy {
        val credential = accountConfig.run {
            val jsonFile = jsonFile
            val pk12File = pk12File
            val serviceAccountEmail = serviceAccountEmail
            val factory = JacksonFactory.getDefaultInstance()

            if (jsonFile != null) {
                GoogleCredential.fromStream(jsonFile.inputStream(), transport, factory)
                        .createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
            } else if (pk12File != null && serviceAccountEmail != null) {
                GoogleCredential.Builder()
                        .setTransport(transport)
                        .setJsonFactory(factory)
                        .setServiceAccountId(serviceAccountEmail)
                        .setServiceAccountPrivateKeyFromP12File(pk12File)
                        .setServiceAccountScopes(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
                        .build()
            } else {
                throw IllegalArgumentException("No credentials provided.")
            }
        }

        AndroidPublisher.Builder(transport, JacksonFactory.getDefaultInstance()) {
            credential.initialize(it.apply {
                readTimeout = 100_000
                connectTimeout = 100_000
            })
        }.setApplicationName(PLUGIN_NAME).build()
    }

    protected fun read(block: AndroidPublisher.Edits.(editId: String) -> Unit) {
        val edits = publisher.edits()
        val request = edits.insert(variant.applicationId, null)

        val id = try {
            request.execute().id
        } catch (e: GoogleJsonResponseException) {
            if (e.details.errors.any { it.reason == "applicationNotFound" }) {
                // Rethrow for clarity
                throw IllegalArgumentException(
                        "No application found for the package name ${variant.applicationId}. " +
                                "The first version of your app must be uploaded via the " +
                                "Play Store console.", e)
            } else {
                throw e
            }
        }

        edits.block(id)
    }

    protected inline fun write(
            crossinline block: AndroidPublisher.Edits.(editId: String) -> Unit
    ) = read {
        block(it)
        commit(variant.applicationId, it).execute()
    }

    internal fun AndroidPublisher.Edits.updateTracks(
            editId: String,
            inputFolder: File,
            releaseStatus: String,
            versions: List<Long>,
            trackType: String,
            userPercent: Double) {
        val track = tracks()
                .list(variant.applicationId, editId)
                .execute().tracks?.firstOrNull { it.track == trackType }
                ?: Track()

        var releaseText: List<LocalizedText>? = null
        if (inputFolder.exists()) {
            releaseText = inputFolder.listFiles(LocaleFileFilter).map { locale ->
                val fileName = ListingDetail.WHATS_NEW.fileName
                val file = run {
                    var file = File(locale, "$fileName-${extension.track}").orNull()
                    if (file == null) file = File(locale, fileName).orNull()
                    file
                } ?: return@map null
                val recentChanges = File(file, fileName).readProcessed(
                        ListingDetail.WHATS_NEW.maxLength,
                        extension.errorOnSizeLimit
                )
                LocalizedText().setLanguage(locale.name).setText(recentChanges)
            }.filterNotNull()
        }
        val trackRelease = TrackRelease().apply {
            releaseNotes = releaseText
            status = releaseStatus
            userFraction = if (status == ReleaseStatus.INPROGRESS.status) userPercent else 0.0
            versionCodes = versions
        }

        track.releases = listOf(trackRelease)

        tracks()
                .update(variant.applicationId, editId, trackType, track)
                .execute()
    }
}
