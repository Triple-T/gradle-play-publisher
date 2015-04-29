package de.triplet.gradle.play

import org.junit.Test

import static org.junit.Assert.assertEquals

class TaskHelperTest {

    private static final File TESTFILE = new File("src/test/fixtures/android_app/src/main/play/en-US/whatsnew")

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
}
