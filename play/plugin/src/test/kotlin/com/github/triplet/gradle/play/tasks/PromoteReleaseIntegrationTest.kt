package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.play.helpers.FakeEditManager
import com.github.triplet.gradle.play.helpers.FakePlayPublisher
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

class PromoteReleaseIntegrationTest : IntegrationTestBase() {
    @Test
    fun `Promote uses standard track by default`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()

            play.track = 'foobar'
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("promoteTrackName=foobar")
    }

    @Test
    fun `Promote uses promote track when specified`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()

            play.track = 'foobar'
            play.promoteTrack = 'not-foobar'
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("promoteTrackName=not-foobar")
    }

    @Test
    fun `Promote finds from track dynamically by default`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=null")
    }

    @Test
    fun `Promote uses from track when specified`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()

            play.fromTrack = 'foobar'
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=foobar")
    }

    @Test
    fun `CLI params can be used to configure task`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()
        """

        val result = execute(
                config,
                "promoteReleaseArtifact",
                "--no-commit",
                "--from-track=myFromTrack",
                "--promote-track=myPromoteTrack",
                "--track=myDefaultTrack",
                "--release-name=myRelName",
                "--release-status=draft",
                "--user-fraction=.88"
        )

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=myFromTrack")
        assertThat(result.output).contains("promoteTrackName=myPromoteTrack")
        assertThat(result.output).contains("releaseName=myRelName")
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertThat(result.output).contains("userFraction=0.88")
    }

    @Test
    fun `CLI params can be used to update track`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()
        """

        val result = execute(config, "promoteReleaseArtifact", "--update=myUpdateTrack")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=myUpdateTrack")
        assertThat(result.output).contains("promoteTrackName=myUpdateTrack")
    }

    @Test
    fun `Build uses correct release status`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()

            play.releaseStatus 'draft'
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseStatus=DRAFT")
    }

    @Test
    fun `Build uses correct release name`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()

            android.buildTypes {
                consoleNames {}
            }
        """

        val result = execute(config, "promoteConsoleNamesArtifact")

        assertThat(result.task(":promoteConsoleNamesArtifact")).isNotNull()
        assertThat(result.task(":promoteConsoleNamesArtifact")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseName=myCustomName")
    }

    @Test
    fun `Build picks default release notes when no track specific ones are available`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()

            android.buildTypes {
                releaseNotes {}
            }
        """

        val result = execute(config, "promoteReleaseNotesArtifact")

        assertThat(result.task(":promoteReleaseNotesArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseNotesArtifact")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseNotes={en-US=My custom release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build picks track specific release notes when available`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()

            android.buildTypes {
                releaseNotes {}
            }

            play.track 'custom-track'
        """

        val result = execute(config, "promoteReleaseNotesArtifact")

        assertThat(result.task(":promoteReleaseNotesArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseNotesArtifact")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseNotes={en-US=Custom track release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build uses correct user fraction`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()

            play.userFraction 0.123
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("userFraction=0.123")
    }

    @Test
    fun `Build uses correct retained artifacts`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()

            play.retain.artifacts = [1, 2, 3]
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("retainableArtifacts=[1, 2, 3]")
    }

    @Test
    fun `Build is not cacheable`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()
        """

        val result1 = execute(config, "promoteReleaseArtifact")
        val result2 = execute(config, "promoteReleaseArtifact")

        assertThat(result1.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result1.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result2.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result1.output).contains("promoteRelease(")
        assertThat(result2.output).contains("promoteRelease(")
    }

    @Test
    fun `Build generates and commits edit by default`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).contains("commitEdit(edit-id)")
    }

    @Test
    fun `Build skips commit when no-commit flag is passed`() {
        @Suppress("UnnecessaryQualifiedReference")
        // language=gradle
        val config = """
            com.github.triplet.gradle.play.tasks.PromoteReleaseIntegrationBridge.installFactories()

            play.commit = false
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).doesNotContain("commitEdit(")
    }
}

object PromoteReleaseIntegrationBridge {
    @JvmStatic
    fun installFactories() {
        val publisher = object : FakePlayPublisher() {
            override fun insertEdit(): EditResponse {
                println("insertEdit()")
                return EditResponse.Success("edit-id")
            }

            override fun getEdit(id: String): EditResponse {
                println("getEdit($id)")
                return EditResponse.Success(id)
            }

            override fun commitEdit(id: String) {
                println("commitEdit($id)")
            }
        }
        val edits = object : FakeEditManager() {
            override fun promoteRelease(
                    promoteTrackName: String,
                    fromTrackName: String?,
                    releaseStatus: ReleaseStatus?,
                    releaseName: String?,
                    releaseNotes: Map<String, String?>?,
                    userFraction: Double?,
                    retainableArtifacts: List<Long>?
            ) {
                println("promoteRelease(" +
                                "promoteTrackName=$promoteTrackName, " +
                                "fromTrackName=$fromTrackName, " +
                                "releaseStatus=$releaseStatus, " +
                                "releaseName=$releaseName, " +
                                "releaseNotes=$releaseNotes, " +
                                "userFraction=$userFraction, " +
                                "retainableArtifacts=$retainableArtifacts)")
            }
        }

        publisher.install()
        edits.install()
    }
}
