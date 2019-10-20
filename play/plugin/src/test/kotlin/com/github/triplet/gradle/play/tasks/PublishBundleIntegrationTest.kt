package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.play.helpers.FakeEditManager
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.helpers.execute
import com.github.triplet.gradle.play.helpers.executeExpectingFailure
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class PublishBundleIntegrationTest : IntegrationTestBase() {
    // TODO finish adding test

    @Test
    fun `Builds bundle on-the-fly by default`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishBundleIntegrationBridge.installFactories()
        """

        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":bundleRelease")).isNotNull()
        assertThat(result.task(":bundleRelease")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle")
        assertThat(result.output).contains("aab")
    }

    @Test
    fun `Rebuilding bundle on-the-fly uses cached build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishBundleIntegrationBridge.installFactories()
        """

        val result1 = execute(config, "publishReleaseBundle")
        val result2 = execute(config, "publishReleaseBundle")

        assertThat(result1.task(":publishReleaseBundle")).isNotNull()
        assertThat(result1.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":publishReleaseBundle")).isNotNull()
        assertThat(result2.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using non-existent custom artifact fails build with warning`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishBundleIntegrationBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        val result = executeExpectingFailure(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Warning")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Using custom artifact with multiple bundles fails build with warning`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishBundleIntegrationBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "1.aab").createNewFile()
        File(tempDir.root, "2.aab").createNewFile()
        val result = executeExpectingFailure(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Warning")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Using custom artifact skips on-the-fly bundle build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishBundleIntegrationBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "foo.aab").createNewFile()
        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":bundleRelease")).isNull()
        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Reusing custom artifact uses cached build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishBundleIntegrationBridge.installFactories()

            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "foo.aab").createNewFile()
        val result1 = execute(config, "publishReleaseBundle")
        val result2 = execute(config, "publishReleaseBundle")

        assertThat(result1.task(":publishReleaseBundle")).isNotNull()
        assertThat(result1.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":publishReleaseBundle")).isNotNull()
        assertThat(result2.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using custom artifact CLI arg skips on-the-fly bundle build`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PublishBundleIntegrationBridge.installFactories()
        """

        File(tempDir.root, "foo.aab").createNewFile()
        val result = execute(config, "publishReleaseBundle", "--artifact-dir=${tempDir.root}")

        assertThat(result.task(":bundleRelease")).isNull()
        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadInternalSharingBundle")
        assertThat(result.output).contains(tempDir.root.name)
    }
}

object PublishBundleIntegrationBridge {
    @JvmStatic
    fun installFactories() {
        val edits = object : FakeEditManager() {
            override fun uploadBundle(
                    bundleFile: File,
                    mappingFile: File?,
                    strategy: ResolutionStrategy,
                    versionCode: Long,
                    variantName: String,
                    isBuildSkippingCommit: Boolean,
                    releaseStatus: ReleaseStatus,
                    trackName: String,
                    retainableArtifacts: List<Long>?,
                    releaseName: String?,
                    releaseNotes: Map<String, String?>,
                    userFraction: Double
            ) {
                println("uploadBundle($bundleFile, $mappingFile, $strategy, $versionCode, " +
                                "$variantName, $isBuildSkippingCommit, $releaseStatus, " +
                                "$trackName, $retainableArtifacts, $releaseName, $releaseNotes, " +
                                "$userFraction)")
            }
        }
        edits.install()
    }
}
