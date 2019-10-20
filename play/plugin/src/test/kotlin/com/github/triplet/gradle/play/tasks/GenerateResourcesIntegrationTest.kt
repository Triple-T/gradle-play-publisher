package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.helpers.FIXTURE_WORKING_DIR
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.helpers.execute
import com.github.triplet.gradle.play.helpers.executeExpectingFailure
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class GenerateResourcesIntegrationTest : IntegrationTestBase() {
    @Test
    fun `Basic resources are correctly copied to their respective folders`() {
        execute("", "generateReleasePlayResources")

        "".exists()
        "release".exists()
        "release/res/listings/en-US".exists()
        "release/res/listings/fr-FR".exists()

        "release/res/release-notes/en-US/default.txt" generated "main"
        "release/res/release-notes/fr-FR/default.txt" generated "main"

        "release/res/products/sku.json" generated "src/main/play/products/sku.json"()
    }

    // TODO(#709): add test making sure source files doesn't change

    @Test
    fun `Product flavors override main variant fallbacks`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }
        """

        execute(
                config,
                "clean",
                "generateFreeReleasePlayResources",
                "generatePaidReleasePlayResources"
        )

        "".exists()
        "freeRelease".exists()
        "freeRelease/res/listings/en-US".exists()
        "freeRelease/res/listings/fr-FR".exists()
        "freeRelease/res/listings/de-DE".exists()

        "freeRelease/res/release-notes/en-US/default.txt" generated "free"
        "freeRelease/res/release-notes/fr-FR/default.txt" generated "main"
        "freeRelease/res/release-notes/de-DE/default.txt" generated "free german"

        "paidRelease/res/release-notes/en-US/default.txt" generated "paid english"
        "paidRelease/res/release-notes/fr-FR/default.txt" generated "main"
        "paidRelease/res/release-notes/de-DE/default.txt" generated "paid german"
    }

    @Test
    fun `Build types override variant fallbacks`() {
        // language=gradle
        val config = """
            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        """

        execute(config, "generateDogfoodPlayResources")

        "dogfood/res/release-notes/en-US/default.txt" generated "dogfood english"
    }

    @Test
    fun `Product flavors override build type fallbacks`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        """

        execute(config, "generatePaidDogfoodPlayResources")

        "paidDogfood/res/release-notes/en-US/default.txt" generated "dogfood english"
    }

    @Test
    fun `Specific flavor overrides build type fallbacks`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        """

        execute(config, "generateFreeDogfoodPlayResources")

        "freeDogfood/res/release-notes/en-US/default.txt" generated "free dogfood english"
    }

    @Test
    fun `Multidimensional resources are not overwritten when merging`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }

            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        """

        execute(config, "generateFreeStagingDogfoodPlayResources")

        "freeStagingDogfood/res/release-notes/en-US/default.txt" generated "freeStagingDogfood"
        "freeStagingDogfood/res/listings/en-US/full-description.txt" generated "freeStagingDogfood"
    }

    @Test
    fun `Multidimensional resources merge across flavors`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """

        execute(config, "generateFreeStagingReleasePlayResources")

        "freeStagingRelease/res/release-notes/en-US/default.txt" generated "freeStaging"
        "freeStagingRelease/res/listings/en-US/full-description.txt" generated "freeStagingRelease"
    }

    @Test
    fun `Multidimensional resources merge across flavors 2`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """

        execute(config, "generateFreeStagingReleasePlayResources")

        "freeStagingRelease/res/listings/en-US/short-description.txt" generated "free"
        "freeStagingRelease/res/listings/en-US/full-description.txt" generated "freeStagingRelease"
    }

    @Test
    fun `Flavor dimension order determines winner when two multidimensional resources conflict`() {
        // language=gradle
        val config = """
            flavorDimensions 'server', 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """

        execute(config, "generateProdFreeReleasePlayResources")

        "prodFreeRelease/res/release-notes/en-US/default.txt" generated "prod"
    }

    @Test
    fun `Main variant merges with other flavors`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'server' }
            }
        """

        execute(config, "generateFreeStagingReleasePlayResources")

        "freeStagingRelease/res/listings/en-US/title.txt" generated "main"
        "freeStagingRelease/res/listings/en-US/full-description.txt" generated "freeStagingRelease"
    }

    @Test
    fun `Resources merge across languages`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """

        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/res/listings/de-DE/title.txt" generated "main"
        "prodStagingRelease/res/listings/de-DE/full-description.txt" generated "de-DE"
        "prodStagingRelease/res/listings/de-DE/short-description.txt" generated "staging"
    }

    @Test
    fun `Graphics language merge only across categories`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """

        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/res/listings/fr-FR/graphics/phone-screenshots/foo.jpg".exists(no)
        "prodStagingRelease/res/listings/fr-FR/graphics/phone-screenshots/bar.jpg".exists()
        "prodStagingRelease/res/listings/fr-FR/graphics/tablet-screenshots/baz.jpg".exists()
        "prodStagingRelease/res/listings/de-DE/graphics/phone-screenshots/foo.jpg".exists()
        "prodStagingRelease/res/listings/de-DE/graphics/tablet-screenshots/baz.jpg".exists()
    }

    @Test
    fun `Hidden files are ignored`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                hiddenFile { dimension 'pricing' }
            }
        """

        val result = execute(config, "generateHiddenFileReleasePlayResources")

        assertThat(result.task(":generateHiddenFileReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `Non-existent locale throws`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                invalidLocale { dimension 'pricing' }
            }
        """

        val result = executeExpectingFailure(
                config, "generateInvalidLocaleReleasePlayResources")

        assertThat(result.task(":generateInvalidLocaleReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.FAILED)
    }

    @Test
    fun `Files in wrong dir throw`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                unknownFile { dimension 'pricing' }
            }
        """

        val result = executeExpectingFailure(
                config, "generateUnknownFileReleasePlayResources")

        assertThat(result.task(":generateUnknownFileReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.FAILED)
    }

    private val yes: (Boolean) -> Unit = { assertThat(it).isTrue() }
    private val no: (Boolean) -> Unit = { assertThat(it).isFalse() }
    private fun String.exists(validator: (Boolean) -> Unit = yes) {
        validator(File(FIXTURE_WORKING_DIR, "build/generated/gpp/$this").exists())
    }

    private infix fun String.generated(content: String) {
        assertThat(content).isEqualTo("build/generated/gpp/$this"())
    }

    private operator fun String.invoke() = File(FIXTURE_WORKING_DIR, this).readText()
}
