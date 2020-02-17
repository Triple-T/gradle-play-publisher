package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import com.github.triplet.gradle.androidpublisher.newUploadInternalSharingArtifactResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class PublishInternalSharingApkIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "PublishInternalSharingApkIntegrationTest.installFactories()"

    @Test
    fun `Builds apk on-the-fly by default`() {
        val result = execute("", "uploadReleasePrivateApk")

        assertThat(result.task(":assembleRelease")).isNotNull()
        assertThat(result.task(":assembleRelease")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(".apk")
    }

    @Test
    fun `Rebuilding apk on-the-fly uses cached build`() {
        val result1 = execute("", "uploadReleasePrivateApk")
        val result2 = execute("", "uploadReleasePrivateApk")

        assertThat(result1.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result1.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result2.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using non-existent custom artifact fails build with warning`() {
        // language=gradle
        val config = """
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
        // language=gradle
        val config = """
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
        // language=gradle
        val config = """
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
        File(tempDir.root, "foo.apk").createNewFile()
        val result = execute("", "uploadReleasePrivateApk", "--artifact-dir=${tempDir.root}")

        assertThat(result.task(":assembleRelease")).isNull()
        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Using custom artifact CLI arg with eager evaluation skips on-the-fly apk build`() {
        // language=gradle
        val config = """
            playConfigs {
                release {
                    track = 'hello'
                }
            }

            tasks.all {}
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
        // language=gradle
        val config = """
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
        val outputDir = File(appDir, "build/outputs/internal-sharing/apk/release")

        val minimumTime = System.currentTimeMillis()
        execute("", "uploadReleasePrivateApk")
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
        val result = execute("", "uploadReleasePrivateApk")

        assertThat(result.output).contains("Upload successful: http")
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
