package de.triplet.gradle.play.internal

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport

const val PLUGIN_NAME = "gradle-play-publisher"
const val PLUGIN_GROUP = "Play Store"

const val ACCOUNT_CONFIG = "playAccountConfig"

const val PLAY_PATH = "play"
const val LISTING_PATH = "listing/"
const val RESOURCES_OUTPUT_PATH = "build/outputs/$PLAY_PATH"

val transport: NetHttpTransport by lazy { GoogleNetHttpTransport.newTrustedTransport() }
val imageExtensions = arrayOf("png", "jpg")
