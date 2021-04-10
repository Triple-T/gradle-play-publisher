package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import com.github.triplet.gradle.androidpublisher.newUploadInternalSharingArtifactResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest.Companion.DEFAULT_TASK_VARIANT
import com.github.triplet.gradle.play.tasks.shared.ArtifactIntegrationTests
import com.github.triplet.gradle.play.tasks.shared.PublishInternalSharingArtifactIntegrationTests
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import java.io.File

class PublishInternalSharingBundleIntegrationTest : IntegrationTestBase(), ArtifactIntegrationTests,
        PublishInternalSharingArtifactIntegrationTests {
    override fun taskName(taskVariant: String) =
            ":upload${taskVariant.ifEmpty { DEFAULT_TASK_VARIANT }}PrivateBundle"

    override fun customArtifactName(name: String) = "$name.aab"

    override fun assertCustomArtifactResults(result: BuildResult, executed: Boolean) {
        assertThat(result.task(":packageReleaseBundle")).isNull()
        if (executed) {
            assertThat(result.output).contains("uploadInternalSharingBundle(")
        }
    }

    override fun outputFile() = "build/outputs/internal-sharing/bundle/release"

    @Test
    fun `Builds bundle on-the-fly by default`() {
        val result = execute("", "uploadReleasePrivateBundle")

        result.requireTask(":packageReleaseBundle", outcome = SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle(")
        assertThat(result.output).contains(".aab")
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun uploadInternalSharingBundle(
                        bundleFile: File,
                ): UploadInternalSharingArtifactResponse {
                    println("uploadInternalSharingBundle($bundleFile)")
                    return newUploadInternalSharingArtifactResponse(
                            "json-payload", "https://google.com")
                }
            }
            publisher.install()
        }
    }
}
