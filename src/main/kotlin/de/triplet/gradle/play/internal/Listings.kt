package de.triplet.gradle.play.internal

import java.io.File
import java.io.FileFilter

internal object LocaleFileFilter : FileFilter {
    // region '419' is a special case in the Play Store that represents latin america
    // 'fil' is a special case in the Play Store that represents Filipino
    override fun accept(file: File) =
            file.name.matches(Regex("^(fil|[a-z]{2}(-([A-Z]{2}|419))?)\\z"))
}

internal object ImageFileFilter : FileFilter {
    override fun accept(file: File) = file.extension.toLowerCase() in imageExtensions
}

internal enum class ListingDetail(val fileName: String, val maxLength: Int = Int.MAX_VALUE) {
    TITLE("title", 50),
    SHORT_DESCRIPTION("shortdescription", 80),
    FULL_DESCRIPTION("fulldescription", 4000),
    VIDEO("video"),

    WHATS_NEW("whatsnew", 500),

    CONTACT_EMAIL("contactEmail"),
    CONTACT_PHONE("contactPhone"),
    CONTACT_WEBSITE("contactWebsite"),
    DEFAULT_LANGUAGE("defaultLanguage");
}

internal enum class ImageType(val fileName: String, val maxNum: Int = 8) {
    ICON("icon", 1),
    FEATURE_GRAPHIC("featureGraphic", 1),
    PROMO_GRAPHIC("promoGraphic", 1),

    PHONE_SCREENSHOTS("phoneScreenshots"),
    SEVEN_INCH_SCREENSHOTS("sevenInchScreenshots"),
    TEN_INCH_SCREENSHOTS("tenInchScreenshots"),
    TV_BANNER("tvBanner", 1),
    TV_SCREENSHOTS("tvScreenshots"),
    WEAR_SCREENSHOTS("wearScreenshots");
}
