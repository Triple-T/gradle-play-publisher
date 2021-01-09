package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import com.github.triplet.gradle.androidpublisher.newUploadInternalSharingArtifactResponse
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest.Companion.DEFAULT_TASK_VARIANT
import com.github.triplet.gradle.play.tasks.shared.ArtifactIntegrationTests
import com.github.triplet.gradle.play.tasks.shared.PublishInternalSharingArtifactIntegrationTests
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import java.io.File

class PublishInternalSharingApkIntegrationTest : IntegrationTestBase(), ArtifactIntegrationTests,
        PublishInternalSharingArtifactIntegrationTests {
    override fun taskName(taskVariant: String) =
            ":upload${taskVariant.ifEmpty { DEFAULT_TASK_VARIANT }}PrivateApk"

    override fun customArtifactName() = "foo.apk"

    override fun assertCustomArtifactResults(result: BuildResult) {
        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.output).contains("uploadInternalSharingApk(")
    }

    override fun outputFile() = "build/outputs/internal-sharing/apk/release"

    @Test
    fun `Builds apk on-the-fly by default`() {
        val result = execute("", "uploadReleasePrivateApk")

        result.requireTask(":packageRelease", outcome = SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(".apk")
    }

    @Test
    fun `Using non-existent custom artifact skips build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        val result = execute(config, "uploadReleasePrivateApk")

        assertThat(result.task(":packageRelease")).isNull()
        result.requireTask(outcome = NO_SOURCE)
    }

    @Test
    fun `Using custom artifact with multiple APKs uploads each one`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "1.apk").safeCreateNewFile()
        File(playgroundDir, "2.apk").safeCreateNewFile()
        val result = execute(config, "uploadReleasePrivateApk")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("1.apk")
        assertThat(result.output).contains("2.apk")
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun uploadInternalSharingApk(
                        apkFile: File
                ): UploadInternalSharingArtifactResponse {
                    println("uploadInternalSharingApk($apkFile)")
                    return newUploadInternalSharingArtifactResponse(
                            "json-payload", "https://google.com")
                }
            }
            publisher.install()
        }
    }
}
