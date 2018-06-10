package com.github.triplet.gradle.play.internal

import com.google.api.services.androidpublisher.model.InAppProduct
import com.google.api.services.androidpublisher.model.InAppProductListing
import com.google.api.services.androidpublisher.model.MonthDay
import com.google.api.services.androidpublisher.model.Price
import com.google.api.services.androidpublisher.model.Prorate
import com.google.api.services.androidpublisher.model.Season
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import java.io.File

internal val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(
                InAppProduct::class.java,
                JsonDeserializer<InAppProduct> { element, _, context ->
                    val json = element.asJsonObject
                    InAppProduct()
                            .setDefaultLanguage(json["defaultLanguage"]?.asString)
                            .setDefaultPrice(context.deserialize(
                                    json["defaultPrice"], Price::class.java))
                            .setListings(json["listings"]?.asJsonObject?.entrySet()?.associate {
                                it.toPair()
                            }?.mapValues {
                                context.deserialize<InAppProductListing>(
                                        it.value, InAppProductListing::class.java)
                            })
                            .setPackageName(json["packageName"]?.asString)
                            .setPrices(json["prices"]?.asJsonObject?.entrySet()?.associate {
                                it.toPair()
                            }?.mapValues {
                                context.deserialize<Price>(it.value, Price::class.java)
                            })
                            .setPurchaseType(json["purchaseType"]?.asString)
                            .setSeason(context.deserialize(json["season"], Season::class.java))
                            .setSku(json["sku"]?.asString)
                            .setStatus(json["status"]?.asString)
                            .setSubscriptionPeriod(json["subscriptionPeriod"]?.asString)
                            .setTrialPeriod(json["trialPeriod"]?.asString)
                }
        )
        .registerTypeAdapter(Season::class.java, JsonDeserializer<Season> { element, _, context ->
            val json = element.asJsonObject
            Season()
                    .setEnd(context.deserialize(json["end"], MonthDay::class.java))
                    .setProrations(json["prorations"]?.asJsonArray?.map {
                        context.deserialize<Prorate>(it, Prorate::class.java)
                    })
                    .setStart(context.deserialize(json["start"], MonthDay::class.java))
        })
        .registerTypeAdapter(Prorate::class.java, JsonDeserializer<Prorate> { element, _, context ->
            val json = element.asJsonObject
            Prorate()
                    .setDefaultPrice(context.deserialize(json["defaultPrice"], Price::class.java))
                    .setStart(context.deserialize(json["start"], MonthDay::class.java))
        })
        .registerTypeAdapter(MonthDay::class.java, JsonDeserializer<MonthDay> { element, _, _ ->
            val json = element.asJsonObject
            MonthDay()
                    .setDay(json["day"]?.asLong)
                    .setMonth(json["month"]?.asLong)
        })
        .create()

internal fun File.orNull() = if (exists()) this else null

internal tailrec fun File.findClosestDir(): File {
    check(exists()) { "$this does not exist" }
    return if (isDirectory) this else parentFile.findClosestDir()
}

internal fun File.climbUpTo(parentName: String): File? =
        if (name == parentName) this else parentFile?.climbUpTo(parentName)

internal fun File.readProcessed(maxLength: Int, error: Boolean) =
        readText().normalized().takeOrThrow(maxLength, error, this)

internal fun File.isChildOf(parentName: String) = climbUpTo(parentName) != null

internal fun File.isDirectChildOf(parentName: String) = parentFile?.name == parentName

internal fun File.safeCreateNewFile() = apply {
    check(parentFile.exists() || parentFile.mkdirs()) { "Unable to create $parentFile" }
    check(exists() || createNewFile()) { "Unable to create $this" }
}

internal fun String.normalized() = replace(Regex("\\r\\n"), "\n").trim()

internal fun String?.nullOrFull() = if (isNullOrBlank()) null else this

private fun String.takeOrThrow(n: Int, error: Boolean, file: File): String {
    val result = take(n)
    if (error) check(result.length == length) {
        "File '$file' has reached the limit of $n characters."
    }
    return result
}
