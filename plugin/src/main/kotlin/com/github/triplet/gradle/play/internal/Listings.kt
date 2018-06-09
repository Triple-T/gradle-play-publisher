package com.github.triplet.gradle.play.internal

import java.io.File
import java.io.FileFilter

internal object LocaleFileFilter : FileFilter {
    // region '419' is a special case in the Play Store that represents latin america
    // 'fil' is a special case in the Play Store that represents Filipino
    private val localeRegex = Regex("^(fil|[a-z]{2}(-([A-Z]{2}|419))?)\\z")

    override fun accept(file: File) = file.name.matches(localeRegex)
}

internal object ImageFileFilter : FileFilter {
    private val imageExtensions = arrayOf("png", "jpg")

    override fun accept(file: File) = file.extension.toLowerCase() in imageExtensions
}

internal object JsonFileFilter : FileFilter {
    private const val JSON_EXTENSION = "json"

    override fun accept(file: File) = file.extension.toLowerCase() == JSON_EXTENSION
}

internal enum class ListingDetail(val fileName: String, val maxLength: Int = Int.MAX_VALUE) {
    TITLE("title", 50),
    SHORT_DESCRIPTION("shortdescription", 80),
    FULL_DESCRIPTION("fulldescription", 4000),
    VIDEO("video"),

    WHATS_NEW("whatsnew", 500)
}

internal enum class AppDetail(val fileName: String, val maxLength: Int = Int.MAX_VALUE) {
    CONTACT_EMAIL("contactEmail"),
    CONTACT_PHONE("contactPhone"),
    CONTACT_WEBSITE("contactWebsite"),
    DEFAULT_LANGUAGE("defaultLanguage")
}

internal enum class ImageType(
        val fileName: String,
        val constraints: ImageSize = ImageSize(320, 320, 3840, 3840),
        val maxNum: Int = 8
) {
    ICON("icon", ImageSize(512, 512), 1),
    FEATURE_GRAPHIC("featureGraphic", ImageSize(1024, 500), 1),
    PROMO_GRAPHIC("promoGraphic", ImageSize(180, 120), 1),

    PHONE_SCREENSHOTS("phoneScreenshots"),
    SEVEN_INCH_SCREENSHOTS("sevenInchScreenshots"),
    TEN_INCH_SCREENSHOTS("tenInchScreenshots"),
    TV_BANNER("tvBanner", ImageSize(1280, 720), 1),
    TV_SCREENSHOTS("tvScreenshots"),
    WEAR_SCREENSHOTS("wearScreenshots")
}

// Min length for any side: 320px. Max length for any side: 3840px.
internal data class ImageSize(
        val minWidth: Int,
        val minHeight: Int,
        val maxWidth: Int = minWidth,
        val maxHeight: Int = minHeight
)
