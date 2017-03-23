package de.triplet.gradle.play

import org.gradle.api.Project
import org.junit.Before
import org.junit.Test

import java.nio.charset.StandardCharsets

import static org.assertj.core.api.Assertions.assertThat
import static org.junit.Assert.fail

class TaskHelperTest {

    private static final TESTFILE = new File(TestHelper.FIXTURE_WORKING_DIR, 'src/main/play/en-US/whatsnew')
    private static final TESTFILE_WITH_LINEBREAK = new File(TestHelper.FIXTURE_WORKING_DIR, 'src/main/play/en-US/listing/shortdescription')
    private static final BROKEN_SINGLE_LINE = new File(TestHelper.FIXTURE_WORKING_DIR, 'src/main/play/defaultLanguage')
    private static final byte[] BYTES_NEW_LINES = [97, 13, 10, 98, 13, 10, 99, 13, 10, 97]

    Project project

    @Before
    void setup() {
        project = TestHelper.evaluatableProject()
    }

    @Test
    void testFilesAreCorrectlyTrimmed() {
        assertThat(TaskHelper.readAndTrimFile(project, TESTFILE, 6, false)).hasSize(6)
    }

    @Test
    void testShortFilesAreNotTrimmed() {
        assertThat(TaskHelper.readAndTrimFile(project, TESTFILE, 100, false)).hasSize(12)
    }

    @Test
    void testCorrectTextLength() {
        TaskHelper.readAndTrimFile(project, TESTFILE, 50, true)
    }

    @Test
    void testIncorrectTextLength() {
        try {
            TaskHelper.readAndTrimFile(project, TESTFILE, 1, true)
            fail()
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageMatching('File \'.+\' has reached the limit of 1 characters')
        }
    }

    @Test
    void testTrailingLinebreakIsCutOff() {
        TaskHelper.readAndTrimFile(project, TESTFILE_WITH_LINEBREAK, 28, true)
    }

    @Test
    void testGetCharacterCount() {
        def message = new String(BYTES_NEW_LINES, StandardCharsets.UTF_8)
        assertThat(message).hasSize(10)
        assertThat(TaskHelper.normalize(message)).hasSize(7)
    }

    @Test
    void testReadSingleLine() {
        assertThat(TaskHelper.readSingleLine(BROKEN_SINGLE_LINE)).isEqualTo('en-US')
    }
}
