package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.newFailureEditResponse
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

class GenerateEditIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "GenerateEditIntegrationTest.installFactories()"

    @Test
    fun `Fresh edit is created by default`() {
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")

        val result = execute("", "generateEditForComDotExampleDotPublisher")

        assertThat(result.task(":generateEditForComDotExampleDotPublisher")).isNotNull()
        assertThat(result.task(":generateEditForComDotExampleDotPublisher")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("getEdit")
        assertThat(result.output).contains("insertEdit(")
        assertThat(editFile.readText()).isEqualTo("edit-id")
    }

    @Test
    fun `Skipped edit is read by default`() {
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")
        editFile.safeCreateNewFile().writeText("foobar")
        editFile.marked("skipped").safeCreateNewFile()

        val result = execute("", "generateEditForComDotExampleDotPublisher")

        assertThat(result.task(":generateEditForComDotExampleDotPublisher")).isNotNull()
        assertThat(result.task(":generateEditForComDotExampleDotPublisher")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("insertEdit")
        assertThat(result.output).contains("getEdit(foobar)")
        assertThat(editFile.readText()).isEqualTo("foobar")
    }

    @Test
    fun `New edit is created if skipped edit is invalid`() {
        // language=gradle
        val config = """
            System.setProperty("FAIL", "true")
        """
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")
        editFile.safeCreateNewFile().writeText("foobar")
        editFile.marked("skipped").safeCreateNewFile()

        val result = execute(config, "generateEditForComDotExampleDotPublisher")

        assertThat(result.task(":generateEditForComDotExampleDotPublisher")).isNotNull()
        assertThat(result.task(":generateEditForComDotExampleDotPublisher")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("getEdit(foobar)")
        assertThat(result.output).contains("insertEdit(")
        assertThat(editFile.readText()).isEqualTo("edit-id")
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
                    return if (System.getProperty("FAIL") == null) {
                        newSuccessEditResponse(id)
                    } else {
                        newFailureEditResponse("editExpired")
                    }
                }
            }
            publisher.install()
        }
    }
}
