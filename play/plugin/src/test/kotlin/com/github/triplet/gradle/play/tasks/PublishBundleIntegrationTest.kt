package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.tasks.shared.ArtifactIntegrationTests
import com.github.triplet.gradle.play.tasks.shared.PublishArtifactIntegrationTests
import com.github.triplet.gradle.play.tasks.shared.PublishOrPromoteArtifactIntegrationTests
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class PublishBundleIntegrationTest : IntegrationTestBase(), ArtifactIntegrationTests,
        PublishOrPromoteArtifactIntegrationTests, PublishArtifactIntegrationTests {
    override fun taskName(taskVariant: String) = ":publish${taskVariant}Bundle"

    override fun customArtifactName() = "foo.aab"

    override fun assertCustomArtifactResults(result: BuildResult) {
        assertThat(result.task(":packageReleaseBundle")).isNull()
        assertArtifactUpload(result)
    }

    override fun assertArtifactUpload(result: BuildResult) {
        assertThat(result.output).contains("uploadBundle(")
    }

    @Test
    fun `Builds bundle on-the-fly by default`() {
        val result = execute("", "publishReleaseBundle")

        result.requireTask(":packageReleaseBundle", outcome = SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains(".aab")
    }

    @Test
    fun `Using non-existent custom artifact fails build with warning`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":packageRelease")).isNull()
        result.requireTask(outcome = TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Using custom artifact with multiple bundles uploads each one`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "1.aab").safeCreateNewFile()
        File(playgroundDir, "2.aab").safeCreateNewFile()
        val result = execute(config, "publishReleaseBundle")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("1.aab")
        assertThat(result.output).contains("2.aab")
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

                override fun commitEdit(id: String) {
                    println("commitEdit($id)")
                }

                override fun validateEdit(id: String) {
                    println("validateEdit($id)")
                }
            }
            val edits = object : FakeEditManager() {
                val versionCodeIndex = AtomicInteger()

                override fun findMaxAppVersionCode(): Long {
                    println("findMaxAppVersionCode()")
                    return 123
                }

                override fun uploadBundle(
                        bundleFile: File,
                        strategy: ResolutionStrategy
                ) : Long {
                    println("uploadBundle(" +
                                    "bundleFile=$bundleFile, " +
                                    "strategy=$strategy)")

                    if (System.getProperty("FAIL") != null) error("Upload failed")

                    val versionCodes = System.getProperty("VERSION_CODES").nullOrFull().orEmpty()
                            .replace(" ", "").ifEmpty { "1" }.split(",")
                            .map { it.toLong() }
                    val index = versionCodeIndex.getAndUpdate {
                        (it + 1).coerceAtMost(versionCodes.size - 1)
                    }
                    return versionCodes[index]
                }

                override fun publishArtifacts(
                        versionCodes: List<Long>,
                        didPreviousBuildSkipCommit: Boolean,
                        trackName: String,
                        releaseStatus: ReleaseStatus?,
                        releaseName: String?,
                        releaseNotes: Map<String, String?>?,
                        userFraction: Double?,
                        updatePriority: Int?,
                        retainableArtifacts: List<Long>?
                ) {
                    println("publishArtifacts(" +
                                    "versionCodes=$versionCodes, " +
                                    "didPreviousBuildSkipCommit=$didPreviousBuildSkipCommit, " +
                                    "trackName=$trackName, " +
                                    "releaseStatus=$releaseStatus, " +
                                    "releaseName=$releaseName, " +
                                    "releaseNotes=$releaseNotes, " +
                                    "userFraction=$userFraction, " +
                                    "updatePriority=$updatePriority, " +
                                    "retainableArtifacts=$retainableArtifacts)")
                }
            }

            publisher.install()
            edits.install()
        }
    }
}
