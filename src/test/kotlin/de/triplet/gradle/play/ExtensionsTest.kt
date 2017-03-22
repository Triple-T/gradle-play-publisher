package de.triplet.gradle.play

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

class ExtensionsTest {
    private val TESTFILE = "src/main/play/en-US/whatsnew"
    private val TESTFILE_WITH_LINEBREAK = "src/main/play/en-US/listing/shortdescription"
    private val BROKEN_SINGLE_LINE = "src/main/play/defaultLanguage"
    private val BYTES_NEW_LINES = byteArrayOf(97, 13, 10, 98, 13, 10, 99, 13, 10, 97)

    private lateinit var project: Project

    @Before
    fun setup() {
        project = ProjectBuilder.builder()
                .withProjectDir(File("src/test/fixtures/android_app"))
                .build()
    }

    @Test
    fun testFilesAreCorrectlyTrimmed() {
        assertThat(project.file(TESTFILE).readAndTrim(project, 6, false)).hasSize(6)
    }

    @Test
    fun testShortFilesAreNotTrimmed() {
        assertThat(project.file(TESTFILE).readAndTrim(project, 100, false)).hasSize(12)
    }

    @Test
    fun testCorrectTextLength() {
        project.file(TESTFILE).readAndTrim(project, 50, true)
    }

    @Test
    fun testIncorrectTextLength() {
        try {
            project.file(TESTFILE).readAndTrim(project, 1, true)
            fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageMatching("File \'.+\' has reached the limit of 1 characters")
        }
    }

    @Test
    fun testTrailingLinebreakIsCutOff() {
        project.file(TESTFILE_WITH_LINEBREAK).readAndTrim(project, 28, true)
    }

    @Test
    fun testGetCharacterCount() {
        val message = String(BYTES_NEW_LINES, StandardCharsets.UTF_8)
        assertThat(message).hasSize(10)
        assertThat(message.normalize()).hasSize(7)
    }

    @Test
    fun testReadSingleLine() {
        assertThat(project.file(BROKEN_SINGLE_LINE).firstLine()).isEqualTo("en-US")
    }
}
