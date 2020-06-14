package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import com.github.triplet.gradle.androidpublisher.newUploadInternalSharingArtifactResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

class InstallInternalSharingArtifactIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "InstallInternalSharingArtifactIntegrationTest.installFactories()"

    @Test
    fun `Build depends on uploading apk artifact by default`() {
        val result = execute("", "installReleasePrivateArtifact")

        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `Build depends on uploading bundle artifact when specified`() {
        // language=gradle
        val config = """
            play.defaultToAppBundles = true
        """

        val result = execute(config, "installReleasePrivateArtifact")

        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `Task is not cacheable`() {
        val result1 = execute("", "installReleasePrivateArtifact")
        val result2 = execute("", "installReleasePrivateArtifact")

        assertThat(result1.task(":installReleasePrivateArtifact")).isNotNull()
        assertThat(result1.task(":installReleasePrivateArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":installReleasePrivateArtifact")).isNotNull()
        assertThat(result2.task(":installReleasePrivateArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `Task launches view intent with artifact URL`() {
        val result = execute("", "installReleasePrivateArtifact")

        assertThat(result.task(":installReleasePrivateArtifact")).isNotNull()
        assertThat(result.task(":installReleasePrivateArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output)
                .contains("am start -a \"android.intent.action.VIEW\" -d myDownloadUrl")
    }

    @Test
    fun `Task fails when shell connection fails`() {
        // language=gradle
        val config = """
            System.setProperty("FAIL", "true")
        """

        val result = executeExpectingFailure(config, "installReleasePrivateArtifact")

        assertThat(result.task(":installReleasePrivateArtifact")).isNotNull()
        assertThat(result.task(":installReleasePrivateArtifact")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Failed to install")
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
                            "{\"downloadUrl\": \"myDownloadUrl\"}", "")
                }

                override fun uploadInternalSharingBundle(
                        bundleFile: File
                ): UploadInternalSharingArtifactResponse {
                    println("uploadInternalSharingBundle($bundleFile)")
                    return newUploadInternalSharingArtifactResponse(
                            "{\"downloadUrl\": \"myDownloadUrl\"}", "")
                }
            }
            val shell = object : InstallInternalSharingArtifact.AdbShell {
                fun install() {
                    val context = this
                    InstallInternalSharingArtifact.AdbShell.setFactory(
                            object : InstallInternalSharingArtifact.AdbShell.Factory {
                                override fun create(adbExecutable: File, timeOutInMs: Int) = context
                            })
                }

                override fun executeShellCommand(command: String): Boolean {
                    println("executeShellCommand($command)")
                    return System.getProperty("FAIL") == null
                }
            }

            publisher.install()
            shell.install()
        }
    }
}
