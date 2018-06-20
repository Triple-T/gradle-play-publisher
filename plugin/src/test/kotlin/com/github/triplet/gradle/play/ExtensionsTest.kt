package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.normalized
import com.github.triplet.gradle.play.internal.readProcessed
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

class ExtensionsTest {
    private lateinit var project: Project

    @Before
    fun setup() {
        project = ProjectBuilder.builder()
                .withProjectDir(File("src/test/fixtures/android_app"))
                .build()
    }

    @Test(expected = IllegalStateException::class)
    fun `Long files throw overflow`() {
        project.file(TEST_FILE).readProcessed(1)
    }

    @Test
    fun `Files on the edge don't throw`() {
        assertThat(project.file(TEST_FILE).readProcessed(4)).hasSize(4)
    }

    @Test
    fun `Short files don't throw`() {
        assertThat(project.file(TEST_FILE).readProcessed(100)).hasSize(4)
    }

    @Test
    fun `Character counts are valid`() {
        assertThat(newLine).hasSize(10)
        assertThat(newLine.normalized()).hasSize(7)
    }

    private companion object {
        const val TEST_FILE = "src/main/play/release-notes/en-US/default.txt"

        val newLine = byteArrayOf(97, 13, 10, 98, 13, 10, 99, 13, 10, 97)
                .toString(StandardCharsets.UTF_8)
    }
}
