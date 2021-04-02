package com.github.triplet.gradle.play.tasks.shared

import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File

interface PublishInternalSharingArtifactIntegrationTests : SharedIntegrationTest {
    fun outputFile(): String

    @Test
    fun `Task outputs file with API response`() {
        val outputDir = File(appDir, outputFile())

        execute("", taskName())

        assertThat(outputDir.listFiles()).isNotNull()
        assertThat(outputDir.listFiles()!!.size).isEqualTo(1)
        assertThat(outputDir.listFiles()!!.first().name).endsWith(".json")
        assertThat(outputDir.listFiles()!!.first().readText()).isEqualTo("json-payload")
    }

    @Test
    fun `Task logs download url to console`() {
        val result = execute("", taskName())

        assertThat(result.output).contains("Upload successful: http")
    }
}
