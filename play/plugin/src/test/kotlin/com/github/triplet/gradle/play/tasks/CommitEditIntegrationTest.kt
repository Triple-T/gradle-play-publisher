package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Test
import java.io.File

class CommitEditIntegrationTest : IntegrationTestBase() {
    @Before
    fun executeFactoryInstallation() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.CommitEditIntegrationTest.installFactories()
        """

        execute(config, "help")
    }

    @Test
    fun `Commit is not applied by default`() {
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")
        editFile.safeCreateNewFile().writeText("foobar")

        val result = execute("", "commitEditForComDotExampleDotPublisher")

        assertThat(result.task(":commitEditForComDotExampleDotPublisher")).isNotNull()
        assertThat(result.task(":commitEditForComDotExampleDotPublisher")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("commitEdit(")
    }

    @Test
    fun `Commit is not applied if skip requested`() {
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")
        editFile.safeCreateNewFile().writeText("foobar")
        editFile.marked("skipped").safeCreateNewFile()

        val result = execute("", "commitEditForComDotExampleDotPublisher")

        assertThat(result.task(":commitEditForComDotExampleDotPublisher")).isNotNull()
        assertThat(result.task(":commitEditForComDotExampleDotPublisher")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("commitEdit(")
    }

    @Test
    fun `Commit is applied if requested`() {
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")
        editFile.safeCreateNewFile().writeText("foobar")
        editFile.marked("commit").safeCreateNewFile()

        val result = execute("", "commitEditForComDotExampleDotPublisher")

        assertThat(result.task(":commitEditForComDotExampleDotPublisher")).isNotNull()
        assertThat(result.task(":commitEditForComDotExampleDotPublisher")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("commitEdit(foobar)")
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun commitEdit(id: String) {
                    println("commitEdit($id)")
                }
            }
            publisher.install()
        }
    }
}
