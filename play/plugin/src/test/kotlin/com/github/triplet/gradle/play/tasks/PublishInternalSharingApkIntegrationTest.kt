package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.UploadInternalSharingArtifactResponse
import com.github.triplet.gradle.androidpublisher.newUploadInternalSharingArtifactResponse
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

class PublishInternalSharingApkIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "PublishInternalSharingApkIntegrationTest.installFactories()"

    @Test
    fun `Builds apk on-the-fly by default`() {
        val result = execute("", "uploadReleasePrivateApk")

        assertThat(result.task(":packageRelease")).isNotNull()
        assertThat(result.task(":packageRelease")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
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
    fun `Using non-existent custom artifact skips build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        val result = execute(config, "uploadReleasePrivateApk")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Using custom artifact skips on-the-fly apk build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "foo.apk").safeCreateNewFile()
        val result = execute(config, "uploadReleasePrivateApk")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains("foo.apk")
    }

    @Test
    fun `Using custom artifact file skips on-the-fly apk build`() {
        val app = File(playgroundDir, "foo.apk").safeCreateNewFile()
        // language=gradle
        val config = """
            play {
                artifactDir = file('${app.escaped()}')
            }
        """

        val result = execute(config, "uploadReleasePrivateApk")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains("foo.apk")
    }

    @Test
    fun `Reusing custom artifact uses cached build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "foo.apk").safeCreateNewFile()
        val result1 = execute(config, "uploadReleasePrivateApk")
        val result2 = execute(config, "uploadReleasePrivateApk")

        assertThat(result1.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result1.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result2.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using custom artifact correctly tracks dependencies`() {
        // language=gradle
        val config = """
            abstract class CustomTask extends DefaultTask {
                @OutputDirectory
                abstract DirectoryProperty getAppDir()

                @TaskAction
                void doStuff() {
                    appDir.get().file("foo.apk").asFile.createNewFile()
                }
            }

            def c = tasks.register("myCustomTask", CustomTask) {
                appDir.set(layout.projectDirectory.dir('${playgroundDir.escaped()}'))
            }

            play {
                artifactDir = c.flatMap { it.appDir }
            }
        """

        val result = execute(config, "uploadReleasePrivateApk")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":myCustomTask")).isNotNull()
        assertThat(result.task(":myCustomTask")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains("foo.apk")
    }

    @Test
    fun `Using custom artifact CLI arg skips on-the-fly apk build`() {
        File(playgroundDir, "foo.apk").safeCreateNewFile()
        val result = execute("", "uploadReleasePrivateApk", "--artifact-dir=${playgroundDir}")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(playgroundDir.name)
    }

    @Test
    fun `Using custom artifact CLI arg with eager evaluation skips on-the-fly apk build`() {
        // language=gradle
        val config = """
            playConfigs {
                release {
                    track.set('hello')
                }
            }

            tasks.all {}
        """.withAndroidBlock()

        File(playgroundDir, "foo.apk").safeCreateNewFile()
        val result = execute(config, "uploadReleasePrivateApk", "--artifact-dir=${playgroundDir}")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":uploadReleasePrivateApk")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingApk(")
        assertThat(result.output).contains(playgroundDir.name)
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
