package com.github.triplet.gradle.play.tasks.shared

import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test

interface PublishArtifactIntegrationTests : SharedIntegrationTest {
    fun assertArtifactUpload(result: BuildResult)

    @Test
    fun `CLI params can be used to configure task`() {
        val result = execute(
                "",
                taskName(),
                "--no-commit",
                "--release-name=myRelName",
                "--release-status=draft",
                "--resolution-strategy=ignore",
                "--track=myCustomTrack",
                "--user-fraction=.88",
                "--update-priority=3"
        )

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("releaseName=myRelName")
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertThat(result.output).contains("strategy=IGNORE")
        assertThat(result.output).contains("trackName=myCustomTrack")
        assertThat(result.output).contains("userFraction=0.88")
        assertThat(result.output).contains("updatePriority=3")
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).doesNotContain("commitEdit(")
        assertArtifactUpload(result)
    }

    @Test
    fun `Global CLI params can be used to configure task`() {
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
                taskName(/* No variant: */ ""),
                "--no-commit",
                "--release-name=myRelName",
                "--release-status=draft",
                "--resolution-strategy=ignore",
                "--track=myCustomTrack",
                "--user-fraction=.88",
                "--update-priority=3"
        )

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("releaseName=myRelName")
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertThat(result.output).contains("strategy=IGNORE")
        assertThat(result.output).contains("trackName=myCustomTrack")
        assertThat(result.output).contains("userFraction=0.88")
        assertThat(result.output).contains("updatePriority=3")
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).doesNotContain("commitEdit(")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build picks track specific release name when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }

            play.track = 'custom-track'
        """

        val result = execute(config, taskName("ConsoleNames"))

        result.requireTask(taskName("ConsoleNames"), outcome = SUCCESS)
        assertThat(result.output).contains("releaseName=myCustomName")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build picks default release notes when no track specific ones are available`() {
        // language=gradle
        val config = """
            buildTypes {
                releaseNotes {}
            }
        """.withAndroidBlock()

        val result = execute(config, taskName("ReleaseNotes"))

        result.requireTask(taskName("ReleaseNotes"), outcome = SUCCESS)
        assertThat(result.output).contains("releaseNotes={en-US=My default release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build picks track specific release notes when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                releaseNotes {}
            }

            play.track = 'custom-track'
        """

        val result = execute(config, taskName("ReleaseNotes"))

        result.requireTask(taskName("ReleaseNotes"), outcome = SUCCESS)
        assertThat(result.output).contains("releaseNotes={en-US=Custom track release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build ignores promote track specific release name when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }

            play.track = 'custom-track'
            play.promoteTrack = 'promote-track'
        """

        val result = execute(config, taskName("ConsoleNames"))

        result.requireTask(taskName("ConsoleNames"), outcome = SUCCESS)
        assertThat(result.output).contains("releaseName=myCustomName")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build ignores promote track specific release notes when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                releaseNotes {}
            }

            play.track = 'custom-track'
            play.promoteTrack = 'promote-track'
        """

        val result = execute(config, taskName("ReleaseNotes"))

        result.requireTask(taskName("ReleaseNotes"), outcome = SUCCESS)
        assertThat(result.output).contains("releaseNotes={en-US=Custom track release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build properly assigns didSkipCommit param when no-commit flag is passed`() {
        // language=gradle
        val config1 = """
            play.commit = false
        """

        // language=gradle
        val config2 = """
            android.defaultConfig.versionCode 2
            play.commit = false
        """

        val result1 = execute(config1, taskName())
        val result2 = execute(config2, taskName())

        result1.requireTask(outcome = SUCCESS)
        assertThat(result1.output).contains("didPreviousBuildSkipCommit=false")
        assertArtifactUpload(result1)

        result2.requireTask(outcome = SUCCESS)
        assertThat(result2.output).contains("didPreviousBuildSkipCommit=true")
        assertArtifactUpload(result2)
    }

    @Test
    fun `Build processes manifest when resolution strategy is set to auto`() {
        // language=gradle
        val config = """
            play.resolutionStrategy = ResolutionStrategy.AUTO
        """

        val result = execute(config, taskName())

        result.requireTask(":processReleaseVersionCodes", outcome = SUCCESS)
        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("findMaxAppVersionCode(")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build fails by default when upload fails`() {
        // language=gradle
        val config = """
            System.setProperty("FAIL", "true")
        """

        val result = executeExpectingFailure(config, taskName())

        result.requireTask(outcome = FAILED)
        assertThat(result.output).contains("Upload failed")
    }

    @Test
    fun `Build uses correct track`() {
        // language=gradle
        val config = """
            play.track = 'myCustomTrack'
        """

        val result = execute(config, taskName())

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("trackName=myCustomTrack")
        assertArtifactUpload(result)
    }
}
