package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.helpers.DefaultPlayPublisher
import com.github.triplet.gradle.play.helpers.execute
import com.google.api.services.androidpublisher.model.InAppProduct
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

class PublishProductsIntegrationTest : IntegrationTestBase() {
    @Test
    fun `Empty dir of products skips task`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationBridge.installFactories()
        """

        val result = execute(config, "publishReleaseProducts")

        assertThat(result.task(":publishReleaseProducts")).isNotNull()
        assertThat(result.task(":publishReleaseProducts")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Invalid file is ignored and task is skipped`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationBridge.installFactories()

            android.buildTypes {
                invalid {}
            }
        """

        val result = execute(config, "publishInvalidProducts")

        assertThat(result.task(":publishInvalidProducts")).isNotNull()
        assertThat(result.task(":publishInvalidProducts")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Hidden file is ignored and task is skipped`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationBridge.installFactories()

            android.buildTypes {
                hidden {}
            }
        """

        val result = execute(config, "publishHiddenProducts")

        assertThat(result.task(":publishHiddenProducts")).isNotNull()
        assertThat(result.task(":publishHiddenProducts")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `File name is used as product SKU`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationBridge.installFactories()

            android.buildTypes {
                nameAsSku {}
            }
        """

        val result = execute(config, "publishNameAsSkuProducts")

        assertThat(result.task(":publishNameAsSkuProducts")).isNotNull()
        assertThat(result.task(":publishNameAsSkuProducts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("sku: \"my-sku\"")
    }
}

object PublishProductsIntegrationBridge {
    @JvmStatic
    fun installFactories() {
        val publisher = object : DefaultPlayPublisher() {
            override fun publishInAppProduct(product: InAppProduct) {
                println("publishInAppProduct($product)")
            }
        }
        publisher.install()
    }
}
