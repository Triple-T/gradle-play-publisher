package de.triplet.gradle.play

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory

internal const val APPLICATION_NAME = "gradle-play-publisher"
internal const val PLAY_STORE_GROUP = "Play Store"

internal const val MIME_TYPE_APK = "application/vnd.android.package-archive"
internal const val MIME_TYPE_IMAGE = "image/*"

internal const val LISTING_PATH = "listing/"
internal const val RESOURCES_OUTPUT_PATH = "build/outputs/play"

internal const val MAX_SCREENSHOTS_SIZE = 8

internal const val MAX_CHARACTER_LENGTH_FOR_TITLE = 50
internal const val MAX_CHARACTER_LENGTH_FOR_SHORT_DESCRIPTION = 80
internal const val MAX_CHARACTER_LENGTH_FOR_FULL_DESCRIPTION = 4000
internal const val MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT = 500

internal const val FILE_NAME_FOR_CONTACT_EMAIL = "contactEmail"
internal const val FILE_NAME_FOR_CONTACT_PHONE = "contactPhone"
internal const val FILE_NAME_FOR_CONTACT_WEBSITE = "contactWebsite"
internal const val FILE_NAME_FOR_DEFAULT_LANGUAGE = "defaultLanguage"

internal const val FILE_NAME_FOR_TITLE = "title"
internal const val FILE_NAME_FOR_SHORT_DESCRIPTION = "shortdescription"
internal const val FILE_NAME_FOR_FULL_DESCRIPTION = "fulldescription"
internal const val FILE_NAME_FOR_VIDEO = "video"
internal const val FILE_NAME_FOR_WHATS_NEW_TEXT = "whatsnew"

internal const val IMAGE_TYPE_FEATURE_GRAPHIC = "featureGraphic"
internal const val IMAGE_TYPE_ICON = "icon"
internal const val IMAGE_TYPE_PHONE_SCREENSHOTS = "phoneScreenshots"
internal const val IMAGE_TYPE_PROMO_GRAPHIC = "promoGraphic"
internal const val IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS = "sevenInchScreenshots"
internal const val IMAGE_TYPE_TEN_INCH_SCREENSHOTS = "tenInchScreenshots"
internal const val IMAGE_TYPE_TV_BANNER = "tvBanner"
internal const val IMAGE_TYPE_TV_SCREENSHOTS = "tvScreenshots"
internal const val IMAGE_TYPE_WEAR_SCREENSHOTS = "wearScreenshots"

internal val JSON_FACTORY by lazy { JacksonFactory.getDefaultInstance() }
internal val HTTP_TRANSPORT by lazy { GoogleNetHttpTransport.newTrustedTransport() }

internal val TRACKS = arrayOf("alpha", "beta", "rollout", "production")
internal val IMAGE_EXTENSIONS = arrayOf("png", "jpg")

internal val IMAGE_TYPES = arrayOf(
        IMAGE_TYPE_ICON,
        IMAGE_TYPE_FEATURE_GRAPHIC,
        IMAGE_TYPE_PHONE_SCREENSHOTS,
        IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS,
        IMAGE_TYPE_TEN_INCH_SCREENSHOTS,
        IMAGE_TYPE_PROMO_GRAPHIC,
        IMAGE_TYPE_TV_BANNER,
        IMAGE_TYPE_TV_SCREENSHOTS,
        IMAGE_TYPE_WEAR_SCREENSHOTS
)

// region '419' is a special case in the play store that represents latin america
// 'fil' is a special case in the play store that represents Filipino
internal val LOCALE_REGEX = Regex("^(fil|[a-z]{2}(-([A-Z]{2}|419))?)\\z")
