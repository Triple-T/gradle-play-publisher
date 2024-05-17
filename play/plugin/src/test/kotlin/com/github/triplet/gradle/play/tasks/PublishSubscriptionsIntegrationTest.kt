package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.UpdateSubscriptionResponse
import com.github.triplet.gradle.androidpublisher.newUpdateSubscriptionResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.github.triplet.gradle.play.tasks.shared.LifecycleIntegrationTests
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Test
import java.io.File

class PublishSubscriptionsIntegrationTest : IntegrationTestBase(), SharedIntegrationTest,
        LifecycleIntegrationTests {
    override fun taskName(taskVariant: String) = ":publish${taskVariant}Subscriptions"

    @Test
    fun `Empty dir of subscriptions skips task`() {
        val result = execute("", "publishReleaseSubscriptions")

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

        val result = execute(config, "publishInvalidSubscriptions")

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

        val result = execute(config, "publishHiddenSubscriptions")

        result.requireTask(taskName("Hidden"), outcome = NO_SOURCE)
    }

    @Test
    fun `Basic subscription publishes`() {
        // language=gradle
        val config = """
            buildTypes {
                simple {}
            }
        """.withAndroidBlock()

        val result = execute(config, "publishSimpleSubscriptions")

        result.requireTask(taskName("Simple"), outcome = SUCCESS)
        assertThat(result.output).contains("updateInAppSubscription(")
        assertThat(result.output).doesNotContain("insertInAppSubscription(")
        assertThat(result.output).contains("subscription.json")
        assertThat(result.output).contains("Uploading subscription")
    }

    @Test
    fun `Multiple subscriptions are all published`() {
        // language=gradle
        val config = """
            buildTypes {
                multipleSubscriptions {}
            }
        """.withAndroidBlock()

        val result = execute(config, "publishMultipleSubscriptionsSubscriptions")

        result.requireTask(taskName("MultipleSubscriptions"), outcome = SUCCESS)
        assertThat(result.output).contains("subscription1")
        assertThat(result.output).contains("subscription2")
    }

    @Test
    fun `Non-existent subscription tries updating then inserts`() {
        // language=gradle
        val config = """
            buildTypes {
                simple {}
            }

            System.setProperty("NEEDS_CREATING", "true")
        """.withAndroidBlock()

        val result = execute(config, "publishSimpleSubscriptions")

        result.requireTask(taskName("Simple"), outcome = SUCCESS)
        assertThat(result.output).contains("updateInAppSubscription(")
        assertThat(result.output).contains("insertInAppSubscription(")
    }

    @Test
    fun `Republishing subscriptions uses cached build`() {
        // language=gradle
        val config = """
            buildTypes {
                multipleSubscriptions {}
            }
        """.withAndroidBlock()

        val result1 = execute(config, "publishMultipleSubscriptionsSubscriptions")
        val result2 = execute(config, "publishMultipleSubscriptionsSubscriptions")

        result1.requireTask(taskName("MultipleSubscriptions"), outcome = SUCCESS)
        result2.requireTask(taskName("MultipleSubscriptions"), outcome = UP_TO_DATE)
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun insertInAppSubscription(subscriptionFile: File, regionsVersion: String) {
                    println("insertInAppSubscription($subscriptionFile)")
                }

                override fun updateInAppSubscription(subscriptionFile: File, regionsVersion: String): UpdateSubscriptionResponse {
                    println("updateInAppSubscription($subscriptionFile)")
                    return newUpdateSubscriptionResponse(System.getProperty("NEEDS_CREATING") != null)
                }
            }
            publisher.install()
        }
    }
}
