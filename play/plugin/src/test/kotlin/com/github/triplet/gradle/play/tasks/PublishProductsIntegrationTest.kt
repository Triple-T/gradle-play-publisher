package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.UpdateProductResponse
import com.github.triplet.gradle.androidpublisher.newUpdateProductResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.github.triplet.gradle.play.tasks.shared.LifecycleIntegrationTests
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Test
import java.io.File

class PublishProductsIntegrationTest : IntegrationTestBase(), SharedIntegrationTest,
        LifecycleIntegrationTests {
    override fun taskName(taskVariant: String) = ":publish${taskVariant}Products"

    @Test
    fun `Empty dir of products skips task`() {
        val result = execute("", "publishReleaseProducts")

        result.requireTask(outcome = NO_SOURCE)
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

        result.requireTask(taskName("Invalid"), outcome = NO_SOURCE)
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

        result.requireTask(taskName("Hidden"), outcome = NO_SOURCE)
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

        result.requireTask(taskName("Simple"), outcome = SUCCESS)
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

        result.requireTask(taskName("MultipleProducts"), outcome = SUCCESS)
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

        result.requireTask(taskName("Simple"), outcome = SUCCESS)
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

        result1.requireTask(taskName("MultipleProducts"), outcome = SUCCESS)
        result2.requireTask(taskName("MultipleProducts"), outcome = UP_TO_DATE)
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
