package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.GppAppDetails
import com.github.triplet.gradle.androidpublisher.GppImage
import com.github.triplet.gradle.androidpublisher.GppListing
import com.github.triplet.gradle.androidpublisher.GppProduct
import com.github.triplet.gradle.androidpublisher.GppSubscription
import com.github.triplet.gradle.androidpublisher.ReleaseNote
import com.github.triplet.gradle.androidpublisher.newGppAppDetails
import com.github.triplet.gradle.androidpublisher.newGppListing
import com.github.triplet.gradle.androidpublisher.newGppProduct
import com.github.triplet.gradle.androidpublisher.newGppSubscription
import com.github.triplet.gradle.androidpublisher.newReleaseNote
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.github.triplet.gradle.play.tasks.shared.LifecycleIntegrationTests
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import java.io.File

class BootstrapIntegrationTest : IntegrationTestBase(), SharedIntegrationTest,
        LifecycleIntegrationTests {
    override fun taskName(taskVariant: String) = ":bootstrap${taskVariant}Listing"

    @Test
    fun `Existing contents get deleted`() {
        File(appDir, "foobar.txt").safeCreateNewFile().writeText("yo")

        val result = execute("", "bootstrapReleaseListing")

        result.requireTask(outcome = SUCCESS)

        "foobar.txt".exists(no)
    }

    @Test
    fun `App details can be bootstrapped`() {
        val result = execute("", "bootstrapReleaseListing", "--app-details")

        result.requireTask(outcome = SUCCESS)

        "default-language.txt" produced "en-US"
        "contact-email.txt" produced "email"
        "contact-phone.txt" produced "phone"
        "contact-website.txt" produced "website"

        "listings".exists(no)
        "release-notes".exists(no)
        "products".exists(no)
        "subscriptions".exists(no)
    }

    @Test
    fun `Listings can be bootstrapped`() {
        val result = execute("", "bootstrapReleaseListing", "--listings")

        result.requireTask(outcome = SUCCESS)

        "listings/en-US/title.txt" produced "title"
        "listings/en-US/full-description.txt" produced "full"
        "listings/en-US/short-description.txt" produced "short"
        "listings/en-US/video-url.txt" produced "url"

        "default-language.txt".exists(no)
        "release-notes".exists(no)
        "products".exists(no)
        "subscriptions".exists(no)
    }

    @Test
    fun `Release notes can be bootstrapped`() {
        val result = execute("", "bootstrapReleaseListing", "--release-notes")

        result.requireTask(outcome = SUCCESS)

        "release-notes/en-US/production.txt" produced "prod"
        "release-notes/fr-FR/alpha.txt" produced "alpha"

        "default-language.txt".exists(no)
        "listings".exists(no)
        "products".exists(no)
        "subscriptions".exists(no)
    }

    @Test
    fun `Products can be bootstrapped`() {
        val result = execute("", "bootstrapReleaseListing", "--products")

        result.requireTask(outcome = SUCCESS)

        "products/sku1.json" produced "product 1"
        "products/sku2.json" produced "product 2"

        "default-language.txt".exists(no)
        "listings".exists(no)
        "release-notes".exists(no)
        "subscriptions".exists(no)
    }

    @Test
    fun `Subscriptions can be bootstrapped`() {
        val result = execute("", "bootstrapReleaseListing", "--subscriptions")

        result.requireTask(outcome = SUCCESS)

        "subscriptions/subscription1.json" produced "product 1"
        "subscriptions/subscription2.json" produced "product 2"

        "default-language.txt".exists(no)
        "listings".exists(no)
        "release-notes".exists(no)
        "products".exists(no)
    }

    private val yes: (Boolean) -> Unit = { assertThat(it).isTrue() }
    private val no: (Boolean) -> Unit = { assertThat(it).isFalse() }
    private fun String.exists(validator: (Boolean) -> Unit = yes) {
        validator(File(appDir, "src/main/play/$this").exists())
    }

    private infix fun String.produced(content: String) {
        assertThat("src/main/play/$this"().trim()).isEqualTo(content)
    }

    private operator fun String.invoke() = File(appDir, this).readText()

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun insertEdit(): EditResponse {
                    println("insertEdit()")
                    return newSuccessEditResponse("edit-id")
                }

                override fun getInAppProducts(): List<GppProduct> {
                    return listOf(
                            newGppProduct("sku1", "product 1"),
                            newGppProduct("sku2", "product 2")
                    )
                }

                override fun getInAppSubscriptions(): List<GppSubscription> {
                    return listOf(
                            newGppSubscription("subscription1", "product 1"),
                            newGppSubscription("subscription2", "product 2")
                    )
                }
            }
            val edits = object : FakeEditManager() {
                override fun getAppDetails(): GppAppDetails {
                    return newGppAppDetails(
                            "en-US", "email", "phone", "website")
                }

                override fun getListings(): List<GppListing> {
                    return listOf(newGppListing("en-US", "full", "short", "title", "url"))
                }

                override fun getImages(locale: String, type: String): List<GppImage> {
                    return emptyList()
                }

                override fun getReleaseNotes(): List<ReleaseNote> {
                    return listOf(
                            newReleaseNote("production", "en-US", "prod"),
                            newReleaseNote("alpha", "fr-FR", "alpha")
                    )
                }
            }

            publisher.install()
            edits.install()
        }
    }
}
