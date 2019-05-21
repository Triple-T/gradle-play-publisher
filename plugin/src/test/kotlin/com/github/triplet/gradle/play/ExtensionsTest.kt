package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.normalized
import com.github.triplet.gradle.play.internal.readProcessed
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

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
        assertEquals(4, project.file(TEST_FILE).readProcessed(4).length)
    }

    @Test
    fun `Short files don't throw`() {
        assertEquals(4, project.file(TEST_FILE).readProcessed(100).length)
    }

    @Test
    fun `Character counts are valid`() {
        assertEquals(10, newLine.length)
        assertEquals(7, newLine.normalized().length)
    }

    private companion object {
        const val TEST_FILE = "src/main/play/release-notes/en-US/default.txt"

        val newLine = byteArrayOf(97, 13, 10, 98, 13, 10, 99, 13, 10, 97)
                .toString(StandardCharsets.UTF_8)
    }
}
