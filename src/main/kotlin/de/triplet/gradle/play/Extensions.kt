package de.triplet.gradle.play

import com.android.build.gradle.AppExtension
import com.android.builder.model.ProductFlavor
import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.AppDetails
import com.google.api.services.androidpublisher.model.Listing
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension
import java.io.File

internal fun String.normalize() = replace(Regex("\\r\\n"), "\n").trim()

internal fun File.readAndTrim(maxLength: Int, errorOnSizeLimit: Boolean, relativeBase: File): String? {
    if (exists()) {
        val message = readText().normalize()

        if (message.length > maxLength) {
            if (errorOnSizeLimit) {
                val relativePath = toRelativeString(relativeBase)
                throw IllegalArgumentException("File '$relativePath' has reached the limit of $maxLength characters")
            }

            return message.substring(0, maxLength).textOrNull()
        }

        return message.textOrNull()
    }

    return null
}

internal fun String?.textOrNull() = if (isNullOrEmpty()) null else this
internal fun String?.orDefault(default: String) = this ?: default

internal fun File.firstLine() = if (exists())
    bufferedReader().lineSequence().first()
else
    null

internal fun File.validSubFolder(vararg path: String): File? {
    var workingFile = this
    path.forEach {
        workingFile = File(workingFile, it)
        if (!workingFile.exists() || !workingFile.mkdirs())
            return null
    }
    return workingFile
}

internal val AppExtension.extensions: ExtensionContainer
    get() {
        return (this as ExtensionAware).extensions
    }

internal val ProductFlavor.extras: ExtraPropertiesExtension
    get() {
        return (this as ExtensionAware).extensions.getByName("ext") as ExtraPropertiesExtension
    }

internal fun File.toApkListing(errorOnSizeLimit: Boolean, relativeBase: File): ApkListing? {
    val listing = ApkListing()
    listing.recentChanges = File(this, ListingDetails.WhatsNew.fileName)
            .readAndTrim(ListingDetails.WhatsNew.maxLength, errorOnSizeLimit, relativeBase)
            ?: return null
    return listing
}

internal fun Listing.saveText(listingDir: File) {
    ListingDetails.FullDescription.saveText(listingDir, fullDescription)
    ListingDetails.ShortDescription.saveText(listingDir, shortDescription)
    ListingDetails.Title.saveText(listingDir, title)
    ListingDetails.Video.saveText(listingDir, video)
}

internal fun File.toListing(errorOnSizeLimit: Boolean, relativeBase: File): Listing {
    val listing = Listing()
    listing.title = File(this, ListingDetails.Title.fileName)
            .readAndTrim(ListingDetails.Title.maxLength, errorOnSizeLimit, relativeBase)
    listing.shortDescription = File(this, ListingDetails.ShortDescription.fileName)
            .readAndTrim(ListingDetails.ShortDescription.maxLength, errorOnSizeLimit, relativeBase)
    listing.fullDescription = File(this, ListingDetails.FullDescription.fileName)
            .readAndTrim(ListingDetails.FullDescription.maxLength, errorOnSizeLimit, relativeBase)
    listing.video = File(this, ListingDetails.Video.fileName).firstLine().textOrNull()
    return listing
}

internal fun AppDetails.saveText(listingDir: File) {
    ListingDetails.ContactEmail.saveText(listingDir, contactEmail)
    ListingDetails.ContactPhone.saveText(listingDir, contactPhone)
    ListingDetails.ContactWebsite.saveText(listingDir, contactWebsite)
    ListingDetails.DefaultLanguage.saveText(listingDir, defaultLanguage)
}

internal fun File.toAppDetails(errorOnSizeLimit: Boolean, relativeBase: File): AppDetails? {
    val details = AppDetails()
    details.defaultLanguage = File(this, ListingDetails.DefaultLanguage.fileName).firstLine().textOrNull() ?: return null
    details.contactEmail = File(this, ListingDetails.ContactEmail.fileName)
            .readAndTrim(ListingDetails.ContactEmail.maxLength, errorOnSizeLimit, relativeBase)
    details.contactPhone = File(this, ListingDetails.ContactPhone.fileName)
            .readAndTrim(ListingDetails.ContactPhone.maxLength, errorOnSizeLimit, relativeBase)
    details.contactWebsite = File(this, ListingDetails.ContactWebsite.fileName)
            .readAndTrim(ListingDetails.ContactWebsite.maxLength, errorOnSizeLimit, relativeBase)
    return details
}

