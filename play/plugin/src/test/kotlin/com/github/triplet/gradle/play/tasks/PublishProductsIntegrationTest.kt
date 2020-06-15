package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.UpdateProductResponse
import com.github.triplet.gradle.androidpublisher.newUpdateProductResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

class PublishProductsIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "PublishProductsIntegrationTest.installFactories()"

    @Test
    fun `Empty dir of products skips task`() {
        val result = execute("", "publishReleaseProducts")

        assertThat(result.task(":publishReleaseProducts")).isNotNull()
        assertThat(result.task(":publishReleaseProducts")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Invalid file is ignored and task is skipped`() {
        // language=gradle
        val config = """
            buildTypes {
                invalid {}
            }
        """.withAndroidBlock()

        val result = execute(config, "publishInvalidProducts")

        assertThat(result.task(":publishInvalidProducts")).isNotNull()
        assertThat(result.task(":publishInvalidProducts")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Hidden file is ignored and task is skipped`() {
        // language=gradle
        val config = """
            buildTypes {
                hidden {}
            }
        """.withAndroidBlock()

        val result = execute(config, "publishHiddenProducts")

        assertThat(result.task(":publishHiddenProducts")).isNotNull()
        assertThat(result.task(":publishHiddenProducts")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Basic product publishes`() {
        // language=gradle
        val config = """
            buildTypes {
                simple {}
            }
        """.withAndroidBlock()

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
        // language=gradle
        val config = """
            buildTypes {
                multipleProducts {}
            }
        """.withAndroidBlock()

        val result = execute(config, "publishMultipleProductsProducts")

        assertThat(result.task(":publishMultipleProductsProducts")).isNotNull()
        assertThat(result.task(":publishMultipleProductsProducts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("sku1")
        assertThat(result.output).contains("sku2")
    }

    @Test
    fun `Non-existent product tries updating then inserts`() {
        // language=gradle
        val config = """
            buildTypes {
                simple {}
            }

            System.setProperty("NEEDS_CREATING", "true")
        """.withAndroidBlock()

        val result = execute(config, "publishSimpleProducts")

        assertThat(result.task(":publishSimpleProducts")).isNotNull()
        assertThat(result.task(":publishSimpleProducts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("updateInAppProduct(")
        assertThat(result.output).contains("insertInAppProduct(")
    }

    @Test
    fun `Republishing products uses cached build`() {
        // language=gradle
        val config = """
            buildTypes {
                multipleProducts {}
            }
        """.withAndroidBlock()

        val result1 = execute(config, "publishMultipleProductsProducts")
        val result2 = execute(config, "publishMultipleProductsProducts")

        assertThat(result1.task(":publishMultipleProductsProducts")).isNotNull()
        assertThat(result1.task(":publishMultipleProductsProducts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":publishMultipleProductsProducts")).isNotNull()
        assertThat(result2.task(":publishMultipleProductsProducts")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
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
