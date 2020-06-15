package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.hash.HashCode
import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

class GenerateResourcesIntegrationTest : IntegrationTestBase() {
    @Test
    fun `Basic resources are correctly copied to their respective folders`() {
        execute("", "generateReleasePlayResources")

        "release/play/listings/en-US".exists()
        "release/play/listings/fr-FR".exists()

        "release/play/release-notes/en-US/default.txt" generated "main"
        "release/play/release-notes/fr-FR/default.txt" generated "main"

        "release/play/products/sku.json" generated "src/main/play/products/sku.json"()
    }

    @Test
    fun `Checksum src files`() {
        val srcDir = File(appDir, "src")
        val srcHash = srcDir.hashAll()

        execute("", "generateReleasePlayResources")

        assertThat(srcDir.hashAll()).isEqualTo(srcHash)
    }

    @Test
    fun `Product flavors override main variant fallbacks`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
            }
        """.withAndroidBlock()

        execute(config, "generateFreeReleasePlayResources", "generatePaidReleasePlayResources")

        "freeRelease/play/listings/en-US".exists()
        "freeRelease/play/listings/fr-FR".exists()
        "freeRelease/play/listings/de-DE".exists()

        "freeRelease/play/release-notes/en-US/default.txt" generated "free"
        "freeRelease/play/release-notes/fr-FR/default.txt" generated "main"
        "freeRelease/play/release-notes/de-DE/default.txt" generated "free german"

        "paidRelease/play/listings/en-US".exists()
        "paidRelease/play/listings/fr-FR".exists()
        "paidRelease/play/listings/de-DE".exists()

        "paidRelease/play/release-notes/en-US/default.txt" generated "paid english"
        "paidRelease/play/release-notes/fr-FR/default.txt" generated "main"
        "paidRelease/play/release-notes/de-DE/default.txt" generated "paid german"
    }

    @Test
    fun `Build types override variant fallbacks`() {
        // language=gradle
        val config = """
            buildTypes {
                dogfood.initWith(buildTypes.release)
            }
        """.withAndroidBlock()

        execute(config, "generateDogfoodPlayResources")

        "dogfood/play/release-notes/en-US/default.txt" generated "dogfood english"
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
        """.withAndroidBlock()

        execute(config, "generatePaidDogfoodPlayResources")

        "paidDogfood/play/release-notes/en-US/default.txt" generated "dogfood english"
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
        """.withAndroidBlock()

        execute(config, "generateFreeDogfoodPlayResources")

        "freeDogfood/play/release-notes/en-US/default.txt" generated "free dogfood english"
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
        """.withAndroidBlock()

        execute(config, "generateFreeStagingDogfoodPlayResources")

        "freeStagingDogfood/play/release-notes/en-US/default.txt" generated "freeStagingDogfood"
        "freeStagingDogfood/play/listings/en-US/full-description.txt" generated "freeStagingDogfood"
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
        """.withAndroidBlock()

        execute(config, "generateFreeStagingReleasePlayResources")

        "freeStagingRelease/play/release-notes/en-US/default.txt" generated "freeStaging"
        "freeStagingRelease/play/listings/en-US/full-description.txt" generated "freeStagingRelease"
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
        """.withAndroidBlock()

        execute(config, "generateFreeStagingReleasePlayResources")

        "freeStagingRelease/play/listings/en-US/short-description.txt" generated "free"
        "freeStagingRelease/play/listings/en-US/full-description.txt" generated "freeStagingRelease"
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
        """.withAndroidBlock()

        execute(config, "generateProdFreeReleasePlayResources")

        "prodFreeRelease/play/release-notes/en-US/default.txt" generated "prod"
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
        """.withAndroidBlock()

        execute(config, "generateFreeStagingReleasePlayResources")

        "freeStagingRelease/play/listings/en-US/title.txt" generated "main"
        "freeStagingRelease/play/listings/en-US/full-description.txt" generated "freeStagingRelease"
    }

    @Test
    fun `Resources are merged across languages`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()

        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/de-DE/title.txt" generated "main"
        "prodStagingRelease/play/listings/de-DE/full-description.txt" generated "de-DE"
        "prodStagingRelease/play/listings/de-DE/short-description.txt" generated "staging"
    }

    @Test
    fun `Child resource merges with parent languages`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        File(appDir, "src/main/play/default-language.txt")("ja-JA")

        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/ja-JA/parent-child-test.txt" generated "staging-pct"
        "prodStagingRelease/play/listings/en-US/parent-child-test.txt" generated "staging-pct"
        "prodStagingRelease/play/listings/fr-FR/parent-child-test.txt" generated "staging-pct"
        "prodStagingRelease/play/listings/de-DE/parent-child-test.txt" generated "staging-pct"
    }

    @Test
    fun `Graphics aren't merged across languages`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()

        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/graphics/phone-screenshots/foo.jpg".exists()
        "prodStagingRelease/play/listings/en-US/graphics/tablet-screenshots/baz.jpg".exists()
        "prodStagingRelease/play/listings/fr-FR/graphics/phone-screenshots/bar.jpg".exists()
        "prodStagingRelease/play/listings/fr-FR/graphics/phone-screenshots/foo.jpg".exists(no)
        "prodStagingRelease/play/listings/fr-FR/graphics/tablet-screenshots/baz.jpg".exists(no)
        "prodStagingRelease/play/listings/de-DE/graphics/phone-screenshots/foo.jpg".exists(no)
        "prodStagingRelease/play/listings/de-DE/graphics/tablet-screenshots/baz.jpg".exists(no)
    }

    @Test
    fun `Top-level graphics are filed into their respective folders`() {
        // language=gradle
        val config = """
            buildTypes {
                topLevelGraphics {}
            }
        """.withAndroidBlock()

        execute(config, "generateTopLevelGraphicsPlayResources")

        "topLevelGraphics/play/listings/en-US/graphics/icon.png".exists(no)
        "topLevelGraphics/play/listings/en-US/graphics/icon/icon.png".exists()
        "topLevelGraphics/play/listings/en-US/graphics/phone-screenshots.png".exists(no)
        "topLevelGraphics/play/listings/en-US/graphics/phone-screenshots/phone-screenshots.png".exists()
        "topLevelGraphics/play/listings/en-US/graphics/phone-screenshots/b.jpeg".exists()
        "topLevelGraphics/play/listings/en-US/graphics/phone-screenshots/foo.jpg".exists()
        "topLevelGraphics/play/listings/en-US/graphics/tablet-screenshots/baz.jpg".exists()
    }

    @Test
    fun `Top-level graphic wins on conflict with filed graphic`() {
        // language=gradle
        val config = """
            buildTypes {
                topLevelGraphicsConflict {}
            }
        """.withAndroidBlock()

        execute(config, "generateTopLevelGraphicsConflictPlayResources")

        "topLevelGraphicsConflict/play/listings/en-US/graphics/phone-screenshots.png".exists(no)
        "topLevelGraphicsConflict/play/listings/en-US/graphics/phone-screenshots/phone-screenshots.png".exists()
        "topLevelGraphicsConflict/play/listings/en-US/graphics/phone-screenshots/foo.jpg".exists()
        "topLevelGraphicsConflict/play/listings/en-US/graphics/tablet-screenshots/baz.jpg".exists()

        "topLevelGraphicsConflict/play/listings/en-US/graphics/phone-screenshots/phone-screenshots.png" generated "top"
    }

    @Test
    fun `Resources aren't merged with no default language`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        File(appDir, "src/main/play/default-language.txt").delete()

        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/de-DE/title.txt".exists(no)
        "prodStagingRelease/play/listings/de-DE/full-description.txt" generated "de-DE"
        "prodStagingRelease/play/listings/de-DE/short-description.txt".exists(no)
    }

    @Test
    fun `Hidden files are ignored`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                hiddenFile { dimension 'pricing' }
            }
        """.withAndroidBlock()

        val result = execute(config, "generateHiddenFileReleasePlayResources")

        assertThat(result.task(":generateHiddenFileReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `Flavor named 'play' is allowed`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                play { dimension 'pricing' }
            }
        """.withAndroidBlock()

        val result = execute(config, "generatePlayReleasePlayResources")

        assertThat(result.task(":generatePlayReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `'play' file name is not allowed`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                illegalPlay { dimension 'pricing' }
            }
        """.withAndroidBlock()

        val result = executeExpectingFailure(
                config, "generateIllegalPlayReleasePlayResources")

        assertThat(result.task(":generateIllegalPlayReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.FAILED)
    }

    @Test
    fun `Non-existent locale throws`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing'
            productFlavors {
                invalidLocale { dimension 'pricing' }
            }
        """.withAndroidBlock()

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
        """.withAndroidBlock()

        val result = executeExpectingFailure(
                config, "generateUnknownFileReleasePlayResources")

        assertThat(result.task(":generateUnknownFileReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.FAILED)
    }

    @Test
    fun `Empty src skips task`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()

        File(appDir, "src").walkTopDown()
                .filter { it.isFile && it.path.contains("play") }
                .forEach { it.delete() }

        val result = execute(config, "generateProdStagingReleasePlayResources")

        assertThat(result.task(":generateProdStagingReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Rerunning task uses cached build`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()

        val result1 = execute(config, "generateProdStagingReleasePlayResources")
        val result2 = execute(config, "generateProdStagingReleasePlayResources")

        assertThat(result1.task(":generateProdStagingReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":generateProdStagingReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Rerunning task with same inputs restores build from cache`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val mainUS = File(appDir, "src/main/play/listings/en-US/incremental.txt")

        val result1 = execute(config, "generateProdStagingReleasePlayResources")
        mainUS("foo")
        val result2 = execute(config, "generateProdStagingReleasePlayResources")
        mainUS.delete()
        val result3 = execute(config, "generateProdStagingReleasePlayResources")

        assertThat(result1.task(":generateProdStagingReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":generateProdStagingReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result3.task(":generateProdStagingReleasePlayResources")!!.outcome)
                .isEqualTo(TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `Incrementally adding file applies across languages`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val mainUS = File(appDir, "src/main/play/listings/en-US/incremental.txt")

        execute(config, "generateProdStagingReleasePlayResources")
        mainUS("en-US incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "en-US incremental"
    }

    @Test
    fun `Incrementally adding file with existing language doesn't overwrite`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val mainUS = File(appDir, "src/main/play/listings/en-US/incremental.txt")
        val mainFR = File(appDir, "src/main/play/listings/fr-FR/incremental.txt")

        mainUS("en-US incremental")
        execute(config, "generateProdStagingReleasePlayResources")
        mainUS("new en-US incremental")
        mainFR("fr-FR incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "new en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "new en-US incremental"
    }

    @Test
    fun `Incrementally adding file with conflicting language doesn't overwrite`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val mainUS = File(appDir, "src/main/play/listings/en-US/incremental.txt")
        val mainFR = File(appDir, "src/main/play/listings/fr-FR/incremental.txt")

        execute(config, "generateProdStagingReleasePlayResources")
        mainUS("en-US incremental")
        mainFR("fr-FR incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "en-US incremental"
    }

    @Test
    fun `Doubly incrementally adding file across variants and languages succeeds`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val stagingUS = File(appDir, "src/staging/play/listings/en-US/incremental.txt")
        val mainFR = File(appDir, "src/main/play/listings/fr-FR/incremental.txt")
        val releaseDE = File(appDir, "src/release/play/listings/de-DE/incremental.txt")
        val prodJA = File(appDir, "src/prod/play/listings/ja-JA/incremental.txt")

        releaseDE("de-DE incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt".exists(no)
        "prodStagingRelease/play/listings/fr-FR/incremental.txt".exists(no)
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt".exists(no)

        mainFR("fr-FR incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt".exists(no)
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt".exists(no)

        stagingUS("en-US incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "en-US incremental"

        prodJA("ja-JA incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "ja-JA incremental"
    }

    @Test
    fun `Incrementally modifying file applies across languages`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val mainUS = File(appDir, "src/main/play/listings/en-US/incremental.txt")

        mainUS("en-US incremental")
        execute(config, "generateProdStagingReleasePlayResources")
        mainUS("new en-US incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "new en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "new en-US incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "new en-US incremental"
    }

    @Test
    fun `Incrementally modifying file with conflicting language doesn't overwrite`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val mainUS = File(appDir, "src/main/play/listings/en-US/incremental.txt")
        val mainFR = File(appDir, "src/main/play/listings/fr-FR/incremental.txt")

        mainUS("en-US incremental")
        mainFR("fr-FR incremental")
        execute(config, "generateProdStagingReleasePlayResources")
        mainUS("new en-US incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "new en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "new en-US incremental"
    }

    @Test
    fun `Doubly incrementally modifying file across variants and languages succeeds`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val stagingUS = File(appDir, "src/staging/play/listings/en-US/incremental.txt")
        val mainFR = File(appDir, "src/main/play/listings/fr-FR/incremental.txt")
        val releaseDE = File(appDir, "src/release/play/listings/de-DE/incremental.txt")
        val prodJA = File(appDir, "src/prod/play/listings/ja-JA/incremental.txt")

        stagingUS("en-US incremental")
        mainFR("fr-FR incremental")
        releaseDE("de-DE incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "en-US incremental"

        mainFR("new fr-FR incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "new fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "en-US incremental"

        stagingUS("new en-US incremental")
        prodJA("ja-JA incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "new en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "new fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "ja-JA incremental"

        releaseDE("new de-DE incremental")
        prodJA("new ja-JA incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "new en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "new fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "new de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "new ja-JA incremental"
    }

    @Test
    fun `Incrementally deleting file applies across languages`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val mainUS = File(appDir, "src/main/play/listings/en-US/incremental.txt")

        mainUS("en-US incremental")
        execute(config, "generateProdStagingReleasePlayResources")
        mainUS.delete()
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt".exists(no)
        "prodStagingRelease/play/listings/fr-FR/incremental.txt".exists(no)
        "prodStagingRelease/play/listings/de-DE/incremental.txt".exists(no)
    }

    @Test
    fun `Incrementally deleting file with existing language doesn't overwrite`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val mainUS = File(appDir, "src/main/play/listings/en-US/incremental.txt")
        val mainFR = File(appDir, "src/main/play/listings/fr-FR/incremental.txt")

        mainUS("en-US incremental")
        mainFR("fr-FR incremental")
        execute(config, "generateProdStagingReleasePlayResources")
        mainUS.delete()
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt".exists(no)
        "prodStagingRelease/play/listings/fr-FR/incremental.txt".exists()
        "prodStagingRelease/play/listings/de-DE/incremental.txt".exists(no)
    }

    @Test
    fun `Doubly incrementally deleting file across variants and languages succeeds`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val stagingUS = File(appDir, "src/staging/play/listings/en-US/incremental.txt")
        val mainFR = File(appDir, "src/main/play/listings/fr-FR/incremental.txt")
        val releaseDE = File(appDir, "src/release/play/listings/de-DE/incremental.txt")

        stagingUS("en-US incremental")
        mainFR("fr-FR incremental")
        releaseDE("de-DE incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "en-US incremental"

        stagingUS.delete()
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt".exists(no)
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt".exists(no)

        mainFR("new fr-FR incremental")
        releaseDE("new de-DE incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt".exists(no)
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "new fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "new de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt".exists(no)
    }

    @Test
    fun `Doubly incrementally adding and deleting file across variants and languages succeeds`() {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'server'
            productFlavors {
                free { dimension 'server' }
                paid { dimension 'pricing' }
                staging { dimension 'server' }
                prod { dimension 'pricing' }
            }
        """.withAndroidBlock()
        val stagingUS = File(appDir, "src/staging/play/listings/en-US/incremental.txt")
        val mainFR = File(appDir, "src/main/play/listings/fr-FR/incremental.txt")
        val releaseDE = File(appDir, "src/release/play/listings/de-DE/incremental.txt")
        val prodJA = File(appDir, "src/prod/play/listings/ja-JA/incremental.txt")

        stagingUS("en-US incremental")
        mainFR("fr-FR incremental")
        releaseDE("de-DE incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "en-US incremental"

        mainFR.delete()
        prodJA("ja-JA incremental")
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "en-US incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "de-DE incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "ja-JA incremental"

        stagingUS.delete()
        mainFR("new fr-FR incremental")
        releaseDE.delete()
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt".exists(no)
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "new fr-FR incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt".exists(no)
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "ja-JA incremental"

        stagingUS("new en-US incremental")
        mainFR.delete()
        execute(config, "generateProdStagingReleasePlayResources")

        "prodStagingRelease/play/listings/en-US/incremental.txt" generated "new en-US incremental"
        "prodStagingRelease/play/listings/fr-FR/incremental.txt" generated "new en-US incremental"
        "prodStagingRelease/play/listings/de-DE/incremental.txt" generated "new en-US incremental"
        "prodStagingRelease/play/listings/ja-JA/incremental.txt" generated "ja-JA incremental"
    }

    private val yes: (Boolean) -> Unit = { assertThat(it).isTrue() }
    private val no: (Boolean) -> Unit = { assertThat(it).isFalse() }
    private fun String.exists(validator: (Boolean) -> Unit = yes) {
        validator(File(appDir, "build/generated/gpp/$this").exists())
    }

    private infix fun String.generated(content: String) {
        assertThat("build/generated/gpp/$this"()).isEqualTo(content)
    }

    private operator fun String.invoke() = File(appDir, this).readText()

    private operator fun File.invoke(text: String) = safeCreateNewFile().writeText(text)

    private fun File.hashAll() = walkTopDown().map {
        val hashes = mutableListOf<HashCode>()
        if (it.isFile) {
            hashes += Files.asByteSource(it).hash(Hashing.sha256())
        }
        hashes += Hashing.sha256().hashUnencodedChars(it.absolutePath)
        hashes
    }.flatten().toList()
}
