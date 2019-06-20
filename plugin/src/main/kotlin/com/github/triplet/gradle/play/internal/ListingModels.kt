package com.github.triplet.gradle.play.internal

internal const val RELEASE_NOTES_DEFAULT_NAME = "default.txt"
internal const val RELEASE_NAMES_DEFAULT_NAME = "default.txt"

internal interface Detail {
    val fileName: String
}

internal enum class ListingDetail(override val fileName: String) : Detail {
    TITLE("title.txt"),
    SHORT_DESCRIPTION("short-description.txt"),
    FULL_DESCRIPTION("full-description.txt"),
    VIDEO("video-url.txt")
}

internal enum class AppDetail(override val fileName: String) : Detail {
    CONTACT_EMAIL("contact-email.txt"),
    CONTACT_PHONE("contact-phone.txt"),
    CONTACT_WEBSITE("contact-website.txt"),
    DEFAULT_LANGUAGE("default-language.txt")
}

internal enum class ImageType(
        val publishedName: String,
        val dirName: String,
        val maxNum: Int = 8
) {
    ICON("icon", "icon", 1),
    FEATURE_GRAPHIC("featureGraphic", "feature-graphic", 1),
    PROMO_GRAPHIC("promoGraphic", "promo-graphic", 1),

    PHONE_SCREENSHOTS("phoneScreenshots", "phone-screenshots"),
    SEVEN_INCH_SCREENSHOTS("sevenInchScreenshots", "tablet-screenshots"),
    TEN_INCH_SCREENSHOTS("tenInchScreenshots", "large-tablet-screenshots"),
    TV_BANNER("tvBanner", "tv-banner", 1),
    TV_SCREENSHOTS("tvScreenshots", "tv-screenshots"),
    WEAR_SCREENSHOTS("wearScreenshots", "wear-screenshots")
}
