package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import com.github.triplet.gradle.play.helpers.FakePlayPublisher
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class PublishInternalSharingApkIntegrationTest : IntegrationTestBase() {
    @Test
    fun `Builds apk on-the-fly by default`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingApkBridge.installFactories()
        """

        val result = execute(config, "uploadReleasePrivateApk")

        assertThat(result.task(":assembleRelease")).isNotNull()
        assertThat(result.task(":assembleRelease")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(".apk")
    }

    @Test
    fun `Rebuilding apk on-the-fly uses cached build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingApkBridge.installFactories()
        """

        val result1 = execute(config, "uploadReleasePrivateApk")
        val result2 = execute(config, "uploadReleasePrivateApk")

        assertThat(result1.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result1.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result2.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using non-existent custom artifact fails build with warning`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingApkBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        val result = executeExpectingFailure(config, "uploadReleasePrivateApk")

        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Warning")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Using custom artifact skips on-the-fly apk build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingApkBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "foo.apk").createNewFile()
        val result = execute(config, "uploadReleasePrivateApk")

        assertThat(result.task(":assembleRelease")).isNull()
        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Reusing custom artifact uses cached build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingApkBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "foo.apk").createNewFile()
        val result1 = execute(config, "uploadReleasePrivateApk")
        val result2 = execute(config, "uploadReleasePrivateApk")

        assertThat(result1.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result1.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result2.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using custom artifact CLI arg skips on-the-fly apk build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingApkBridge.installFactories()
        """

        File(tempDir.root, "foo.apk").createNewFile()
        val result = execute(config, "uploadReleasePrivateApk", "--artifact-dir=${tempDir.root}")

        assertThat(result.task(":assembleRelease")).isNull()
        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Using custom artifact with multiple APKs uploads each one`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingApkBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "1.apk").createNewFile()
        File(tempDir.root, "2.apk").createNewFile()
        val result = execute(config, "uploadReleasePrivateApk")

        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("1.apk")
        assertThat(result.output).contains("2.apk")
    }

    @Test
    fun `Task outputs file with API response`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingApkBridge.installFactories()
        """
        val outputDir = File(appDir, "build/outputs/internal-sharing/apk/release")

        val minimumTime = System.currentTimeMillis()
        execute(config, "uploadReleasePrivateApk")
        val maximumTime = System.currentTimeMillis()

        assertThat(outputDir.listFiles()).isNotNull()
        assertThat(outputDir.listFiles()!!.size).isEqualTo(1)
        assertThat(outputDir.listFiles()!!.first().name).endsWith(".json")
        assertThat(outputDir.listFiles()!!.first().name).isGreaterThan(minimumTime.toString())
        assertThat(outputDir.listFiles()!!.first().name).isLessThan(maximumTime.toString())
        assertThat(outputDir.listFiles()!!.first().readText()).isEqualTo("json-payload")
    }

    @Test
    fun `Task logs download url to console`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingApkBridge.installFactories()
        """

        val result = execute(config, "uploadReleasePrivateApk")

        assertThat(result.output).contains("Upload successful: http")
    }
}

object PublishInternalSharingApkBridge {
    @JvmStatic
    fun installFactories() {
        val publisher = object : FakePlayPublisher() {
            override fun uploadInternalSharingApk(apkFile: File): UploadInternalSharingArtifactResponse {
                println("uploadInternalSharingApk($apkFile)")
                return UploadInternalSharingArtifactResponse("json-payload", "https://google.com")
            }
        }
        publisher.install()
    }
}
