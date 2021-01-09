package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.newFailureEditResponse
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

class GenerateEditIntegrationTest : IntegrationTestBase() {
    @Test
    fun `Fresh edit is created by default`() {
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")

        val result = configureLazyEditGeneration()

        assertThat(result.task(":gen")).isNotNull()
        assertThat(result.task(":gen")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).doesNotContain("getEdit")
        assertThat(result.output).contains("insertEdit(")
        assertThat(editFile.readText()).isEqualTo("edit-id")
    }

    @Test
    fun `Skipped edit is read by default`() {
        val editFile = File(appDir, "build/gpp/com.example.publisher.txt")
        editFile.safeCreateNewFile().writeText("foobar")
        editFile.marked("skipped").safeCreateNewFile()

        val result = configureLazyEditGeneration()

        assertThat(result.task(":gen")).isNotNull()
        assertThat(result.task(":gen")!!.outcome)
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

        val result = configureLazyEditGeneration(config)

        assertThat(result.task(":gen")).isNotNull()
        assertThat(result.task(":gen")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("getEdit(foobar)")
        assertThat(result.output).contains("insertEdit(")
        assertThat(editFile.readText()).isEqualTo("edit-id")
    }

    private fun configureLazyEditGeneration(additionalConfig: String = ""): BuildResult {
        // language=gradle
        val config = """
            import com.github.triplet.gradle.play.tasks.internal.PlayApiService

            tasks.register("gen") { gen ->
                def service = gradle.sharedServices.registrations
                            .named("playApi-com.example.publisher")
                            .get().getService().get() as PlayApiService

                doLast {
                    service.getEdits()
                }
            }

            $additionalConfig
        """
        return execute(config, "gen")
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
            val edits = object : FakeEditManager() {
            }

            publisher.install()
            edits.install()
        }
    }
}
