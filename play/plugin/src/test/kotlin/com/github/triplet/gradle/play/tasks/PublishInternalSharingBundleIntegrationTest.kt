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

class PublishInternalSharingBundleIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "PublishInternalSharingBundleIntegrationTest.installFactories()"

    @Test
    fun `Builds bundle on-the-fly by default`() {
        val result = execute("", "uploadReleasePrivateBundle")

        assertThat(result.task(":packageReleaseBundle")).isNotNull()
        assertThat(result.task(":packageReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle(")
        assertThat(result.output).contains(".aab")
    }

    @Test
    fun `Rebuilding bundle on-the-fly uses cached build`() {
        val result1 = execute("", "uploadReleasePrivateBundle")
        val result2 = execute("", "uploadReleasePrivateBundle")

        assertThat(result1.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result1.task(":uploadReleasePrivateBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result2.task(":uploadReleasePrivateBundle")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using non-existent custom artifact fails build with warning`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        val result = executeExpectingFailure(config, "uploadReleasePrivateBundle")

        assertThat(result.task(":packageReleaseBundle")).isNull()
        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("ERROR_no-unique-aab-found")
        assertThat(result.output).contains(playgroundDir.name)
    }

    @Test
    fun `Using custom artifact with multiple bundles fails build with warning`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "1.aab").safeCreateNewFile()
        File(playgroundDir, "2.aab").safeCreateNewFile()
        val result = executeExpectingFailure(config, "uploadReleasePrivateBundle")

        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("ERROR_no-unique-aab-found")
        assertThat(result.output).contains(playgroundDir.name)
    }

    @Test
    fun `Using custom artifact skips on-the-fly bundle build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "foo.aab").safeCreateNewFile()
        val result = execute(config, "uploadReleasePrivateBundle")

        assertThat(result.task(":packageReleaseBundle")).isNull()
        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle(")
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains("foo.aab")
    }

    @Test
    fun `Using custom artifact file skips on-the-fly bundle build`() {
        val app = File(playgroundDir, "foo.aab").safeCreateNewFile()
        // language=gradle
        val config = """
            play {
                artifactDir = file('${app.escaped()}')
            }
        """

        val result = execute(config, "uploadReleasePrivateBundle")

        assertThat(result.task(":packageReleaseBundle")).isNull()
        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle(")
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains("foo.aab")
    }

    @Test
    fun `Reusing custom artifact uses cached build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "foo.aab").safeCreateNewFile()
        val result1 = execute(config, "uploadReleasePrivateBundle")
        val result2 = execute(config, "uploadReleasePrivateBundle")

        assertThat(result1.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result1.task(":uploadReleasePrivateBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result2.task(":uploadReleasePrivateBundle")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
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
                    appDir.get().file("foo.aab").asFile.createNewFile()
                }
            }

            def c = tasks.register("myCustomTask", CustomTask) {
                appDir.set(layout.projectDirectory.dir('${playgroundDir.escaped()}'))
            }

            play {
                artifactDir = c.flatMap { it.appDir }
            }
        """

        val result = execute(config, "uploadReleasePrivateBundle")

        assertThat(result.task(":packageReleaseBundle")).isNull()
        assertThat(result.task(":myCustomTask")).isNotNull()
        assertThat(result.task(":myCustomTask")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle(")
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains("foo.aab")
    }

    @Test
    fun `Using custom artifact CLI arg skips on-the-fly bundle build`() {
        File(playgroundDir, "foo.aab").safeCreateNewFile()
        val result = execute("", "uploadReleasePrivateBundle", "--artifact-dir=${playgroundDir}")

        assertThat(result.task(":packageReleaseBundle")).isNull()
        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle(")
        assertThat(result.output).contains(playgroundDir.name)
    }

    @Test
    fun `Using custom artifact CLI arg with eager evaluation skips on-the-fly bundle build`() {
        // language=gradle
        val config = """
            playConfigs {
                release {
                    track.set('hello')
                }
            }

            tasks.all {}
        """.withAndroidBlock()

        File(playgroundDir, "foo.aab").safeCreateNewFile()
        val result = execute(config, "uploadReleasePrivateBundle", "--artifact-dir=${playgroundDir}")

        assertThat(result.task(":packageReleaseBundle")).isNull()
        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle(")
        assertThat(result.output).contains(playgroundDir.name)
    }

    @Test
    fun `Task outputs file with API response`() {
        val outputDir = File(appDir, "build/outputs/internal-sharing/bundle/release")

        val minimumTime = System.currentTimeMillis()
        execute("", "uploadReleasePrivateBundle")
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
        val result = execute("", "uploadReleasePrivateBundle")

        assertThat(result.output).contains("Upload successful: http")
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun uploadInternalSharingBundle(
                        bundleFile: File
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
