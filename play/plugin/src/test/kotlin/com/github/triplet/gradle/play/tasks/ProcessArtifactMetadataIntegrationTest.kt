package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ProcessArtifactMetadataIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "ProcessArtifactMetadataIntegrationTest.installFactories()"

    @Test
    fun `Task only runs on release`() {
        val result = execute("", "assembleDebug")

        assertThat(result.task(":processDebugMetadata")).isNull()
    }

    @Test
    fun `Task doesn't run by default`() {
        val result = execute("", "assembleRelease")

        assertThat(result.task(":processReleaseMetadata")).isNotNull()
        assertThat(result.task(":processReleaseMetadata")!!.outcome).isEqualTo(TaskOutcome.SKIPPED)
    }

    @Disabled("https://github.com/Triple-T/gradle-play-publisher/issues/790") // TODO
    @Test
    fun `Version code is incremented and output processor is run with updated version code`() {
        // language=gradle
        val config = """
            play.resolutionStrategy = 'auto'
            play.outputProcessor { output ->
                println("versionCode=" + output.versionCode)
            }
        """

        val result = execute(config, "assembleRelease")

        assertThat(result.task(":processReleaseMetadata")).isNotNull()
        assertThat(result.task(":processReleaseMetadata")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("versionCode=42")
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
                    return newSuccessEditResponse(id)
                }
            }
            val edits = object : FakeEditManager() {
                override fun findMaxAppVersionCode(): Long = 41
            }

            publisher.install()
            edits.install()
        }
    }
}
