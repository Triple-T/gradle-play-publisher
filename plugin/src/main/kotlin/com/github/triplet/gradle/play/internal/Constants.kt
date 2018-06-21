package com.github.triplet.gradle.play.internal

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport

internal const val PLUGIN_NAME = "gradle-play-publisher"
internal const val PLUGIN_GROUP = "Play Store"

internal const val ACCOUNT_CONFIG = "playAccountConfig"

internal const val PLAY_PATH = "play"
internal const val LISTINGS_PATH = "listings"
internal const val RELEASE_NOTES_PATH = "release-notes"
internal const val RESOURCES_OUTPUT_PATH = "generated/gpp"

internal val transport: NetHttpTransport by lazy { GoogleNetHttpTransport.newTrustedTransport() }
