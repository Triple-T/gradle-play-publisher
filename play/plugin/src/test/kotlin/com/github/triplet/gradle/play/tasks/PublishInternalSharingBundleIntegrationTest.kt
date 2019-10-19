package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.helpers.DefaultPlayPublisher
import com.github.triplet.gradle.play.helpers.FIXTURE_WORKING_DIR
import com.github.triplet.gradle.play.helpers.execute
import com.github.triplet.gradle.play.helpers.executeExpectingFailure
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class PublishInternalSharingBundleIntegrationTest : IntegrationTestBase() {
    @Test
    fun `Builds bundle on-the-fly by default`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingBundleBridge.installFactories()
        """

        val result = execute(config, "uploadReleasePrivateBundle")

        assertThat(result.task(":bundleRelease")).isNotNull()
        assertThat(result.task(":bundleRelease")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle")
        assertThat(result.output).contains("aab")
    }

    @Test
    fun `Task outputs file with API response`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingBundleBridge.installFactories()
        """
        val outputDir = File(FIXTURE_WORKING_DIR, "build/outputs/internal-sharing/bundle/release")

        val minimumTime = System.currentTimeMillis()
        execute(config, "uploadReleasePrivateBundle")
        val maximumTime = System.currentTimeMillis()

        assertThat(outputDir.listFiles()).isNotNull()
        assertThat(outputDir.listFiles()!!.size).isEqualTo(1)
        assertThat(outputDir.listFiles()!!.first().name).endsWith(".json")
        assertThat(outputDir.listFiles()!!.first().name).isGreaterThan(minimumTime.toString())
        assertThat(outputDir.listFiles()!!.first().name).isLessThan(maximumTime.toString())
        assertThat(outputDir.listFiles()!!.first().readText())
                .isEqualTo("uploadInternalSharingBundle output")
    }

    @Test
    fun `Using non-existent custom artifact fails build with warning`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingBundleBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        val result = executeExpectingFailure(config, "uploadReleasePrivateBundle")

        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Warning")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Using custom artifact with multiple bundles fails build with warning`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingBundleBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "1.aab").createNewFile()
        File(tempDir.root, "2.aab").createNewFile()
        val result = executeExpectingFailure(config, "uploadReleasePrivateBundle")

        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Warning")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Using custom artifact skips on-the-fly bundle build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingBundleBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "foo.aab").createNewFile()
        val result = execute(config, "uploadReleasePrivateBundle")

        assertThat(result.task(":bundleRelease")).isNull()
        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Using custom artifact CLI arg skips on-the-fly bundle build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishInternalSharingBundleBridge.installFactories()
        """

        File(tempDir.root, "foo.aab").createNewFile()
        val result = execute(
                config,
                "clean",
                "uploadReleasePrivateBundle",
                "--artifact-dir=${tempDir.root}"
        )

        assertThat(result.task(":bundleRelease")).isNull()
        assertThat(result.task(":uploadReleasePrivateBundle")).isNotNull()
        assertThat(result.task(":uploadReleasePrivateBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle")
        assertThat(result.output).contains(tempDir.root.name)
    }
}

object PublishInternalSharingBundleBridge {
    @JvmStatic
    fun installFactories() {
        val publisher = object : DefaultPlayPublisher() {
            override fun uploadInternalSharingBundle(bundleFile: File): String {
                println("uploadInternalSharingBundle($bundleFile)")
                return "uploadInternalSharingBundle output"
            }
        }
        publisher.install()
    }
}
