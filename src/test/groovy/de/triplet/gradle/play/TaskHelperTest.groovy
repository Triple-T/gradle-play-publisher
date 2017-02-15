package de.triplet.gradle.play

import org.junit.Test

import java.nio.charset.Charset

import static org.junit.Assert.assertEquals

class TaskHelperTest {

    private static final File TESTFILE = new File("src/test/fixtures/android_app/src/main/play/en-US/whatsnew")
    private static final File TESTFILE_WITH_LINEBREAK = new File("src/test/fixtures/android_app/src/main/play/en-US/listing/shortdescription")
    private static final File BROKEN_SINGLE_LINE = new File("src/test/fixtures/android_app/src/main/play/defaultLanguage")
    private static final byte[] BYTES_NEW_LINES = [97, 13, 10, 98, 13, 10, 99, 13, 10, 97]

    @Test
    public void testFilesAreCorrectlyTrimmed() {
        def trimmed = TaskHelper.readAndTrimFile(TESTFILE, 6, false)

        assertEquals(6, trimmed.length())
    }

    @Test
    public void testShortFilesAreNotTrimmed() {
        def trimmed = TaskHelper.readAndTrimFile(TESTFILE, 100, false)

        assertEquals(12, trimmed.length())
    }

    @Test
    public void testCorrectTextLength() {
        TaskHelper.readAndTrimFile(TESTFILE, 50, true)
    }

    @Test(expected = LimitExceededException.class)
    public void testIncorrectTextLength() {
        TaskHelper.readAndTrimFile(TESTFILE, 1, true)
    }

    @Test
    public void testTrailingLinebreakIsCutOff() {
        TaskHelper.readAndTrimFile(TESTFILE_WITH_LINEBREAK, 28, true)
    }

    @Test
    public void testGetCharacterCount() {
        def message = new String(BYTES_NEW_LINES, Charset.forName("UTF-8"))
        assertEquals(10, message.length())
        assertEquals(7, TaskHelper.normalize(message).length())
    }

    @Test
    public void testReadSingleLine() {
        assertEquals('en-US', TaskHelper.readSingleLine(BROKEN_SINGLE_LINE))
    }
}
