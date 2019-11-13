package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.UpdateProductResponse
import com.github.triplet.gradle.androidpublisher.newUpdateProductResponse
import com.github.triplet.gradle.play.helpers.FakePlayPublisher
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class PublishProductsIntegrationTest : IntegrationTestBase() {
    @Test
    fun `Empty dir of products skips task`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationTest.installFactories()
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
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationTest.installFactories()

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
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationTest.installFactories()

            android.buildTypes {
                hidden {}
            }
        """

        val result = execute(config, "publishHiddenProducts")

        assertThat(result.task(":publishHiddenProducts")).isNotNull()
        assertThat(result.task(":publishHiddenProducts")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Basic product publishes`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationTest.installFactories()

            android.buildTypes {
                simple {}
            }
        """

        val result = execute(config, "publishSimpleProducts")

        assertThat(result.task(":publishSimpleProducts")).isNotNull()
        assertThat(result.task(":publishSimpleProducts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("updateInAppProduct(")
        assertThat(result.output).doesNotContain("insertInAppProduct(")
        assertThat(result.output).contains("product.json")
        assertThat(result.output).contains("Uploading my-sku")
    }

    @Test
    fun `Multiple products are all published`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationTest.installFactories()

            android.buildTypes {
                multipleProducts {}
            }
        """

        val result = execute(config, "publishMultipleProductsProducts")

        assertThat(result.task(":publishMultipleProductsProducts")).isNotNull()
        assertThat(result.task(":publishMultipleProductsProducts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("sku1")
        assertThat(result.output).contains("sku2")
    }

    @Test
    fun `Non-existent product tries updating then inserts`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationTest.installFactories()

            android.buildTypes {
                simple {}
            }

            System.setProperty("NEEDS_CREATING", "true")
        """

        val result = execute(config, "publishSimpleProducts")

        assertThat(result.task(":publishSimpleProducts")).isNotNull()
        assertThat(result.task(":publishSimpleProducts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("updateInAppProduct(")
        assertThat(result.output).contains("insertInAppProduct(")
    }

    @Test
    fun `Republishing products uses cached build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishProductsIntegrationTest.installFactories()

            android.buildTypes {
                multipleProducts {}
            }
        """

        val result = execute(config, "publishMultipleProductsProducts")

        assertThat(result.task(":publishMultipleProductsProducts")).isNotNull()
        assertThat(result.task(":publishMultipleProductsProducts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("sku1")
        assertThat(result.output).contains("sku2")
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun insertInAppProduct(productFile: File) {
                    println("insertInAppProduct($productFile)")
                }

                override fun updateInAppProduct(productFile: File): UpdateProductResponse {
                    println("updateInAppProduct($productFile)")
                    return newUpdateProductResponse(System.getProperty("NEEDS_CREATING") != null)
                }
            }
            publisher.install()
        }
    }
}
