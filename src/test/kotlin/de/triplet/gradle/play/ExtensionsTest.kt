package de.triplet.gradle.play

import de.triplet.gradle.play.internal.normalized
import de.triplet.gradle.play.internal.readProcessed
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

    @Test
    fun `Long files are trimmed`() {
        assertThat(project.file(TEST_FILE).readProcessed(6, false)).hasSize(6)
    }

    @Test
    fun `Files on the edge are trimmed correctly`() {
        assertThat(project.file(TEST_FILE).readProcessed(12, false)).hasSize(12)
    }

    @Test
    fun `Short files aren't trimmed`() {
        assertThat(project.file(TEST_FILE).readProcessed(100, false)).hasSize(12)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throws on overflow`() {
        project.file(TEST_FILE).readProcessed(1, true)
    }

    @Test
    fun `File is trimmed`() {
        project.file(FILE_WITH_LINEBREAK).readProcessed(28, true)
    }

    @Test
    fun `Character counts are valid`() {
        assertThat(newLine).hasSize(10)
        assertThat(newLine.normalized()).hasSize(7)
    }

    private companion object {
        const val TEST_FILE = "src/main/play/en-US/whatsnew"
        const val FILE_WITH_LINEBREAK = "src/main/play/en-US/listing/shortdescription"

        val newLine = byteArrayOf(97, 13, 10, 98, 13, 10, 99, 13, 10, 97)
                .toString(StandardCharsets.UTF_8)
    }
}
