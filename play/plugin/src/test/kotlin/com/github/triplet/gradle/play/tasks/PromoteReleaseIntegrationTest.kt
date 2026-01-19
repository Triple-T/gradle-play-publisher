package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.CommitResponse
import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.newSuccessCommitResponse
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.github.triplet.gradle.play.helpers.SharedIntegrationTest.Companion.DEFAULT_TASK_VARIANT
import com.github.triplet.gradle.play.tasks.shared.LifecycleIntegrationTests
import com.github.triplet.gradle.play.tasks.shared.PublishOrPromoteArtifactIntegrationTests
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PromoteReleaseIntegrationTest : IntegrationTestBase(), SharedIntegrationTest,
        PublishOrPromoteArtifactIntegrationTests, LifecycleIntegrationTests {
    override fun taskName(taskVariant: String) = ":promote${taskVariant}Artifact"

    override fun assertArtifactUpload(result: BuildResult) {
        assertThat(result.output).contains("promoteRelease(")
    }

    @Test
    fun `Promote uses promote track by default`() {
        // language=gradle
        val config = """
            play.promoteTrack = 'foobar'
        """

        val result = execute(config, "promoteReleaseArtifact")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=auto-track")
        assertThat(result.output).contains("promoteTrackName=foobar")
    }

    @Test
    fun `Promote uses promote track even with track specified`() {
        // language=gradle
        val config = """
            play.track = 'foobar'
            play.promoteTrack = 'not-foobar'
        """

        val result = execute(config, "promoteReleaseArtifact")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("promoteTrackName=not-foobar")
    }

    @Test
    fun `Promote finds from track dynamically by default`() {
        val result = execute("", "promoteReleaseArtifact")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=auto-track")
        assertThat(result.output).contains("promoteTrackName=auto-track")
    }

    @Test
    fun `Promote uses from track when specified`() {
        // language=gradle
        val config = """
            play.fromTrack = 'foobar'
        """

        val result = execute(config, "promoteReleaseArtifact")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=foobar")
        assertThat(result.output).contains("promoteTrackName=foobar")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", DEFAULT_TASK_VARIANT])
    fun `CLI params can be used to configure task`(taskVariant: String) {
        // language=gradle
        val config = """
            playConfigs {
                release {
                    track.set('unused')
                }
            }
        """.withAndroidBlock()

        val result = execute(
                config,
                taskName(taskVariant),
                "--no-commit",
                "--from-track=myFromTrack",
                "--promote-track=myPromoteTrack",
                "--release-name=myRelName",
                "--release-status=draft",
                "--user-fraction=.88",
                "--update-priority=3"
        )

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=myFromTrack")
        assertThat(result.output).contains("promoteTrackName=myPromoteTrack")
        assertThat(result.output).contains("releaseName=myRelName")
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertThat(result.output).contains("userFraction=0.88")
        assertThat(result.output).contains("updatePriority=3")
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).doesNotContain("commitEdit(")
    }

    @Test
    fun `CLI params can be used to update track`() {
        val result = execute("", "promoteReleaseArtifact", "--update=myUpdateTrack")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=myUpdateTrack")
        assertThat(result.output).contains("promoteTrackName=myUpdateTrack")
    }

    @Test
    fun `Build succeeds when mapping file is produced but unavailable`() {
        // language=gradle
        val config = """
            buildTypes.release {
                shrinkResources true
                minifyEnabled true
            }
        """.withAndroidBlock()

        val result = execute(config, ":promoteReleaseArtifact")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
    }

    @Test
    fun `Build picks track specific release name when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }

            play.promoteTrack = 'custom-track'
        """

        val result = execute(config, "promoteConsoleNamesArtifact")

        result.requireTask(taskName("ConsoleNames"), outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseName=myCustomName")
    }

    @Test
    fun `Build picks promote track specific release name when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }

            play.track = 'custom-track'
            play.promoteTrack = 'promote-track'
        """

        val result = execute(config, "promoteConsoleNamesArtifact")

        result.requireTask(taskName("ConsoleNames"), outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseName=myPromoteName")
    }

    @Test
    fun `Build picks default release notes when no track specific ones are available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                releaseNotes {}
            }

            System.setProperty("AUTOTRACK", "other")
        """

        val result = execute(config, "promoteReleaseNotesArtifact")

        result.requireTask(taskName("ReleaseNotes"), outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseNotes={en-US=My default release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build picks track specific release notes when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                releaseNotes {}
            }

            play.fromTrack = 'custom-track'
        """

        val result = execute(config, "promoteReleaseNotesArtifact")

        result.requireTask(taskName("ReleaseNotes"), outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseNotes={en-US=Custom track release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build picks remote track specific release notes when available`() {
        // language=gradle
        val config = """
            buildTypes {
                releaseNotes {}
            }
        """.withAndroidBlock()

        val result = execute(config, "promoteReleaseNotesArtifact")

        result.requireTask(taskName("ReleaseNotes"), outcome = SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseNotes={en-US=Auto track release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build is not cacheable`() {
        val result1 = execute("", "promoteReleaseArtifact")
        val result2 = execute("", "promoteReleaseArtifact")

        result1.requireTask(outcome = SUCCESS)
        result2.requireTask(outcome = SUCCESS)
        assertThat(result1.output).contains("promoteRelease(")
        assertThat(result2.output).contains("promoteRelease(")
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

                override fun commitEdit(id: String, sendChangesForReview: Boolean): CommitResponse {
                    println("commitEdit($id, $sendChangesForReview)")
                    return newSuccessCommitResponse()
                }

                override fun validateEdit(id: String) {
                    println("validateEdit($id)")
                }
            }
            val edits = object : FakeEditManager() {
                override fun findLeastStableTrackName(): String {
                    println("findLeastStableTrackName()")
                    return System.getProperty("AUTOTRACK") ?: "auto-track"
                }

                override fun promoteRelease(
                        promoteTrackName: String,
                        fromTrackName: String,
                        releaseStatus: ReleaseStatus?,
                        releaseName: String?,
                        releaseNotes: Map<String, String?>?,
                        userFraction: Double?,
                        updatePriority: Int?,
                        retainableArtifacts: List<Long>?,
                        versionCode: Long?,
                ) {
                    println("promoteRelease(" +
                                    "promoteTrackName=$promoteTrackName, " +
                                    "fromTrackName=$fromTrackName, " +
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
