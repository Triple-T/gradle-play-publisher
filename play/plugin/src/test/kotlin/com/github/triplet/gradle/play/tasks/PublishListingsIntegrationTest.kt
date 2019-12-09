package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class PublishListingsIntegrationTest : IntegrationTestBase() {
    @Test
    fun `Empty dir of listings skips task`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()
        """

        val result = execute(config, "publishReleaseListing")

        assertThat(result.task(":publishReleaseListing")).isNotNull()
        assertThat(result.task(":publishReleaseListing")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Invalid file is ignored and task is skipped`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()

            android.buildTypes {
                invalid {}
            }
        """

        val result = execute(config, "publishInvalidListing")

        assertThat(result.task(":publishInvalidListing")).isNotNull()
        assertThat(result.task(":publishInvalidListing")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Hidden file is ignored and task is skipped`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()

            android.buildTypes {
                hidden {}
            }
        """

        val result = execute(config, "publishHiddenListing")

        assertThat(result.task(":publishHiddenListing")).isNotNull()
        assertThat(result.task(":publishHiddenListing")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Publishing app details succeeds`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()

            android.buildTypes {
                details {}
            }
        """

        val result = execute(config, "publishDetailsListing")

        assertThat(result.task(":publishDetailsListing")).isNotNull()
        assertThat(result.task(":publishDetailsListing")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("publishListing")
        assertThat(result.output).doesNotContain("publishImages")
        assertThat(result.output).contains("publishAppDetails(")
        assertThat(result.output).contains("defaultLanguage=en-US")
        assertThat(result.output).contains("contactEmail=email")
        assertThat(result.output).contains("contactPhone=phone")
        assertThat(result.output).contains("contactWebsite=https://alexsaveau.dev")
    }

    @Test
    fun `Publishing single listing succeeds`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()

            android.buildTypes {
                singleListing {}
            }
        """

        val result = execute(config, "publishSingleListingListing")

        assertThat(result.task(":publishSingleListingListing")).isNotNull()
        assertThat(result.task(":publishSingleListingListing")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("publishAppDetails")
        assertThat(result.output).doesNotContain("publishImages")
        assertThat(result.output).contains("publishListing(")
        assertThat(result.output).contains("locale=en-US")
        assertThat(result.output).contains("title=title")
        assertThat(result.output).contains("shortDescription=short")
        assertThat(result.output).contains("fullDescription=full")
        assertThat(result.output).contains("video=url")
    }

    @Test
    fun `Publishing multiple listing locales succeeds`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()

            android.buildTypes {
                multiLangListing {}
            }
        """

        val result = execute(config, "publishMultiLangListingListing")

        assertThat(result.task(":publishMultiLangListingListing")).isNotNull()
        assertThat(result.task(":publishMultiLangListingListing")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("publishAppDetails")
        assertThat(result.output).doesNotContain("publishImages")
        assertThat(result.output).contains("publishListing(")
        assertThat(result.output).contains("locale=en-US, title=Lang A")
        assertThat(result.output).contains("locale=fr-FR, title=Lang B")
        assertThat(result.output).contains("locale=de-DE, title=Lang C")
    }

    @Test
    fun `Publishing single graphic succeeds`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()

            android.buildTypes {
                singleGraphics {}
            }
        """

        val result = execute(config, "publishSingleGraphicsListing")

        assertThat(result.task(":publishSingleGraphicsListing")).isNotNull()
        assertThat(result.task(":publishSingleGraphicsListing")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("publishAppDetails")
        assertThat(result.output).doesNotContain("publishListing")
        assertThat(result.output).contains("publishImages(")
        assertThat(result.output).contains("locale=en-US")
        assertThat(result.output).contains("type=icon")
        assertThat(result.output).contains("1.png")
    }

    @Test
    fun `Publishing ignores graphics already on server`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()

            android.buildTypes {
                singleGraphics {}
            }

            System.setProperty(
                    "HASHES", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        """

        val result = execute(config, "publishSingleGraphicsListing")

        assertThat(result.task(":publishSingleGraphicsListing")).isNotNull()
        assertThat(result.task(":publishSingleGraphicsListing")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("publishAppDetails")
        assertThat(result.output).doesNotContain("publishListing")
        assertThat(result.output).doesNotContain("publishImages")
    }

    @Test
    fun `Publishing multiple graphics succeeds`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()

            android.buildTypes {
                multiGraphics {}
            }
        """

        val result = execute(config, "publishMultiGraphicsListing")

        assertThat(result.task(":publishMultiGraphicsListing")).isNotNull()
        assertThat(result.task(":publishMultiGraphicsListing")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("publishAppDetails")
        assertThat(result.output).doesNotContain("publishListing")
        assertThat(result.output).contains("publishImages(")
        assertThat(result.output).contains("locale=en-US")
        assertThat(result.output).contains("type=icon")
        assertThat(result.output).contains("1.png")
        assertThat(result.output).contains("type=phoneScreenshots")
        assertThat(result.output).contains("a.png")
        assertThat(result.output).contains("b.jpeg")
        assertThat(result.output).contains("c.png")
    }

    @Test
    fun `Publishing multiple graphic locales succeeds`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()

            android.buildTypes {
                multiLangGraphics {}
            }
        """

        val result = execute(config, "publishMultiLangGraphicsListing")

        assertThat(result.task(":publishMultiLangGraphicsListing")).isNotNull()
        assertThat(result.task(":publishMultiLangGraphicsListing")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("publishAppDetails")
        assertThat(result.output).doesNotContain("publishListing")
        assertThat(result.output).contains("publishImages(")
        assertThat(result.output).contains("locale=en-US")
        assertThat(result.output).contains("type=icon")
        assertThat(result.output).contains("1.png")
        assertThat(result.output).contains("locale=fr-FR")
        assertThat(result.output).contains("type=phoneScreenshots")
        assertThat(result.output).contains("a.png")
    }

    @Test
    fun `Publishing all metadata succeeds`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishListingsIntegrationTest.installFactories()

            android.buildTypes {
                everything {}
            }
        """

        val result = execute(config, "publishEverythingListing")

        assertThat(result.task(":publishEverythingListing")).isNotNull()
        assertThat(result.task(":publishEverythingListing")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishAppDetails(")
        assertThat(result.output).contains("defaultLanguage=en-US")

        assertThat(result.output).contains("publishListing(")
        assertThat(result.output).contains("locale=en-US")
        assertThat(result.output).contains("title=Title")
        assertThat(result.output).contains("publishImages(")
        assertThat(result.output).contains("type=icon")
        assertThat(result.output).contains("1.png")

        assertThat(result.output).contains("locale=fr-FR")
        assertThat(result.output).contains("fullDescription=Full")
        assertThat(result.output).contains("type=phoneScreenshots")
        assertThat(result.output).contains("a.png")
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun insertEdit(): EditResponse {
                    println("insertEdit()")
                    return newSuccessEditResponse("edit-id")
                }

                override fun getEdit(id: String): EditResponse {
                    println("getEdit($id)")
                    return newSuccessEditResponse(id)
                }

                override fun commitEdit(id: String) {
                    println("commitEdit($id)")
                }
            }
            val edits = object : FakeEditManager() {
                override fun fetchImageHashes(locale: String, type: String): List<String> {
                    println("fetchImageHashes(locale=$locale, type=$type)")
                    return System.getProperty("HASHES")?.split(",") ?: emptyList()
                }

                override fun publishAppDetails(
                        defaultLanguage: String?,
                        contactEmail: String?,
                        contactPhone: String?,
                        contactWebsite: String?
                ) {
                    println("publishAppDetails(defaultLanguage=$defaultLanguage, " +
                                    "contactEmail=$contactEmail, " +
                                    "contactPhone=$contactPhone, " +
                                    "contactWebsite=$contactWebsite)")
                }

                override fun publishListing(
                        locale: String,
                        title: String?,
                        shortDescription: String?,
                        fullDescription: String?,
                        video: String?
                ) {
                    println("publishListing(locale=$locale, " +
                                    "title=$title, " +
                                    "shortDescription=$shortDescription, " +
                                    "fullDescription=$fullDescription, " +
                                    "video=$video)")
                }

                override fun publishImages(locale: String, type: String, images: List<File>) {
                    println("publishImages(locale=$locale, type=$type, images=$images)")
                }
            }

            publisher.install()
            edits.install()
        }
    }
}
