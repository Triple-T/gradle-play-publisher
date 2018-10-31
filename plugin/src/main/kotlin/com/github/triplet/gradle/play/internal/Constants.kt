package com.github.triplet.gradle.play.internal

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport

internal const val PLUGIN_NAME = "gradle-play-publisher"
internal const val PLUGIN_GROUP = "Publishing"

internal const val PLAY_PATH = "play"
internal const val LISTINGS_PATH = "listings"
internal const val GRAPHICS_PATH = "graphics"
internal const val RELEASE_NOTES_PATH = "release-notes"
internal const val PRODUCTS_PATH = "products"
internal const val RESOURCES_OUTPUT_PATH = "generated/gpp"

internal const val MIME_TYPE_STREAM = "application/octet-stream"

internal val transport: NetHttpTransport by lazy { GoogleNetHttpTransport.newTrustedTransport() }
