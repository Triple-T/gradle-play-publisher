package com.github.triplet.gradle.play.helpers

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

interface SharedIntegrationTest : IntegrationTest {
    fun taskName(taskVariant: String = DEFAULT_TASK_VARIANT): String

    fun BuildResult.requireTask(task: String = taskName(), outcome: TaskOutcome) {
        assertThat(task(task)).isNotNull()
        assertThat(task(task)!!.outcome).isEqualTo(outcome)
    }

    companion object {
        const val DEFAULT_TASK_VARIANT = "Release"
    }
}
