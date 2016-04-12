package de.triplet.gradle.play

import org.junit.Test

class ImageFileFilterTest extends GroovyTestCase {

    @Test
    void testJpg_returnsTrue() {
        ImageFileFilter filter = new ImageFileFilter()
        File file = new File("banana.jpg");

        boolean result = filter.accept(file);

        assertTrue(result);
    }

    @Test
    void testUppercaseJpg_returnsTrue() {
        ImageFileFilter filter = new ImageFileFilter()

        File file = new File("banana.JPG");

        boolean result = filter.accept(file);

        assertTrue(result);
    }


    @Test
    void testPng_returnsTrue() {
        ImageFileFilter filter = new ImageFileFilter()
        File file = new File("banana.png");

        boolean result = filter.accept(file);

        assertTrue(result);
    }

    @Test
    void testUppercasePng_returnsTrue() {
        ImageFileFilter filter = new ImageFileFilter()

        File file = new File("banana.PNG");

        boolean result = filter.accept(file);

        assertTrue(result);
    }

    @Test
    void testJpeg_returnsFalse() {
        ImageFileFilter filter = new ImageFileFilter()
        File file = new File("banana.jpeg");

        boolean result = filter.accept(file);

        assertFalse(result);
    }

    @Test
    void testGif_returnsFalse() {
        ImageFileFilter filter = new ImageFileFilter()
        File file = new File("banana.gif");

        boolean result = filter.accept(file);

        assertFalse(result);
    }

    @Test
    void testTiff_returnsFalse() {
        ImageFileFilter filter = new ImageFileFilter()
        File file = new File("banana.tiff");

        boolean result = filter.accept(file);

        assertFalse(result);
    }

    @Test
    void testBmp_returnsFalse() {
        ImageFileFilter filter = new ImageFileFilter()
        File file = new File("banana.bmp");

        boolean result = filter.accept(file);

        assertFalse(result);
    }

    @Test
    void testSvg_returnsFalse() {
        ImageFileFilter filter = new ImageFileFilter()
        File file = new File("banana.svg");

        boolean result = filter.accept(file);

        assertFalse(result);
    }
}
