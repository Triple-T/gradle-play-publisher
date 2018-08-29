package com.github.triplet.gradle.play.internal

import java.io.File
import java.io.FileFilter

internal const val RELEASE_NOTES_DEFAULT_NAME = "default.txt"
internal const val RELEASE_NOTES_MAX_LENGTH = 500

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

internal interface Detail {
    val fileName: String
}

internal enum class ListingDetail(
        override val fileName: String,
        val maxLength: Int = Int.MAX_VALUE
) : Detail {
    TITLE("title.txt", 50),
    SHORT_DESCRIPTION("short-description.txt", 80),
    FULL_DESCRIPTION("full-description.txt", 4000),
    VIDEO("video-url.txt")
}

internal enum class AppDetail(
        override val fileName: String,
        val maxLength: Int = Int.MAX_VALUE
) : Detail {
    CONTACT_EMAIL("contact-email.txt"),
    CONTACT_PHONE("contact-phone.txt"),
    CONTACT_WEBSITE("contact-website.txt"),
    DEFAULT_LANGUAGE("default-language.txt")
}

internal enum class ImageType(
        val publishedName: String,
        val dirName: String,
        val constraints: ImageSize = ImageSize(320, 320, 3840, 3840),
        val maxNum: Int = 8
) {
    ICON("icon", "icon", ImageSize(512, 512), 1),
    FEATURE_GRAPHIC("featureGraphic", "feature-graphic", ImageSize(1024, 500), 1),
    PROMO_GRAPHIC("promoGraphic", "promo-graphic", ImageSize(180, 120), 1),

    PHONE_SCREENSHOTS("phoneScreenshots", "phone-screenshots"),
    SEVEN_INCH_SCREENSHOTS("sevenInchScreenshots", "tablet-screenshots"),
    TEN_INCH_SCREENSHOTS("tenInchScreenshots", "large-tablet-screenshots"),
    TV_BANNER("tvBanner", "tv-banner", ImageSize(1280, 720), 1),
    TV_SCREENSHOTS("tvScreenshots", "tv-screenshots"),
    WEAR_SCREENSHOTS("wearScreenshots", "wear-screenshots")
}

// Min length for any side: 320px. Max length for any side: 3840px.
internal data class ImageSize(
        val minWidth: Int,
        val minHeight: Int,
        val maxWidth: Int = minWidth,
        val maxHeight: Int = minHeight
)
