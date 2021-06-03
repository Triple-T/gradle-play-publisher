package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.CommitResponse
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.newSuccessCommitResponse
import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import java.io.File

class CommitEditIntegrationTest : IntegrationTestBase(), SharedIntegrationTest {
    override fun taskName(taskVariant: String) = ":commitEditForComDotExampleDotPublisher"

    @Test
    fun `Commit is not applied by default`() {
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")
        editFile.safeCreateNewFile().writeText("foobar")

        val result = execute("", ":commitEditForComDotExampleDotPublisher")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).doesNotContain("commitEdit(")
    }

    @Test
    fun `Commit is not applied if skip requested`() {
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")
        editFile.safeCreateNewFile().writeText("foobar")
        editFile.marked("skipped").safeCreateNewFile()

        val result = execute("", ":commitEditForComDotExampleDotPublisher")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).doesNotContain("commitEdit(")
        assertThat(result.output).contains("validateEdit(")
    }

    @Test
    fun `Commit is applied if requested`() {
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")
        editFile.safeCreateNewFile().writeText("foobar")
        editFile.marked("commit").safeCreateNewFile()

        val result = execute("", "commitEditForComDotExampleDotPublisher")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("commitEdit(foobar")
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun commitEdit(id: String, sendChangesForReview: Boolean): CommitResponse {
                    println("commitEdit($id, $sendChangesForReview)")
                    return newSuccessCommitResponse()
                }

                override fun validateEdit(id: String) {
                    println("validateEdit($id)")
                }
            }
            publisher.install()
        }
    }
}
