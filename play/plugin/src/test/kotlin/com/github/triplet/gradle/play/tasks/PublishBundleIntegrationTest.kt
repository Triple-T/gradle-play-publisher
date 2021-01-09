package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.tasks.shared.ArtifactIntegrationTests
import com.github.triplet.gradle.play.tasks.shared.PublishArtifactIntegrationTests
import com.github.triplet.gradle.play.tasks.shared.PublishOrPromoteArtifactIntegrationTests
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import java.io.File

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

        val result = executeExpectingFailure(config, "publishReleaseBundle")

        result.requireTask(outcome = FAILED)
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
        val result = executeExpectingFailure(config, "publishReleaseBundle")

        result.requireTask(outcome = FAILED)
        assertThat(result.output).contains("ERROR_no-unique-aab-found")
        assertThat(result.output).contains(playgroundDir.name)
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
                override fun findMaxAppVersionCode(): Long {
                    println("findMaxAppVersionCode()")
                    return 123
                }

                override fun uploadBundle(
                        bundleFile: File,
                        strategy: ResolutionStrategy,
                        didPreviousBuildSkipCommit: Boolean,
                        trackName: String,
                        releaseStatus: ReleaseStatus?,
                        releaseName: String?,
                        releaseNotes: Map<String, String?>?,
                        userFraction: Double?,
                        updatePriority: Int?,
                        retainableArtifacts: List<Long>?
                ) {
                    println("uploadBundle(" +
                                    "bundleFile=$bundleFile, " +
                                    "strategy=$strategy, " +
                                    "didPreviousBuildSkipCommit=$didPreviousBuildSkipCommit, " +
                                    "trackName=$trackName, " +
                                    "releaseStatus=$releaseStatus, " +
                                    "releaseName=$releaseName, " +
                                    "releaseNotes=$releaseNotes, " +
                                    "userFraction=$userFraction, " +
                                    "updatePriority=$updatePriority, " +
                                    "retainableArtifacts=$retainableArtifacts)")

                    if (System.getProperty("FAIL") != null) error("Upload failed")
                }
            }

            publisher.install()
            edits.install()
        }
    }
}
