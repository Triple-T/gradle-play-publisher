package com.github.triplet.gradle.play.tasks.shared

import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test

interface PublishOrPromoteArtifactIntegrationTests : SharedIntegrationTest {
    fun assertArtifactUpload(result: BuildResult)

    @Test
    fun `Build generates and commits edit by default`() {
        val result = execute("", taskName())

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).contains("commitEdit(edit-id")
    }

    @Test
    fun `Build skips commit when no-commit flag is passed`() {
        // language=gradle
        val config = """
            play.commit = false
        """

        val result = execute(config, taskName())

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).doesNotContain("commitEdit(")
        assertThat(result.output).contains("validateEdit(")
    }

    @Test
    fun `Build uses correct release status`() {
        // language=gradle
        val config = """
            play.releaseStatus = ReleaseStatus.DRAFT
        """

        val result = execute(config, taskName())

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build picks default release name when no track specific ones are available`() {
        // language=gradle
        val config = """
            buildTypes {
                consoleNames {}
            }
        """.withAndroidBlock()

        val result = execute(config, taskName("ConsoleNames"))

        result.requireTask(taskName("ConsoleNames"), outcome = SUCCESS)
        assertThat(result.output).contains("releaseName=myDefaultName")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build uses correct user fraction`() {
        // language=gradle
        val config = """
            play.userFraction = 0.123d
        """

        val result = execute(config, taskName())

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("userFraction=0.123")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build uses correct update priority`() {
        // language=gradle
        val config = """
            play.updatePriority = 5
        """

        val result = execute(config, taskName())

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("updatePriority=5")
        assertArtifactUpload(result)
    }

    @Test
    fun `Build uses correct retained artifacts`() {
        // language=gradle
        val config = """
            play.retain.artifacts = [1l, 2l, 3l]
        """

        val result = execute(config, taskName())

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("retainableArtifacts=[1, 2, 3]")
        assertArtifactUpload(result)
    }
}
