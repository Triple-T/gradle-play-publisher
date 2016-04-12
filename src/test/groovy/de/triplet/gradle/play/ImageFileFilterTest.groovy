package de.triplet.gradle.play

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ImageFileFilterTest {

    ImageFileFilter filter = new ImageFileFilter()

    @Test
    void testJpg_returnsTrue() {
        assertTrue(filter.accept(new File("banana.jpg")))
    }

    @Test
    void testUppercaseJpg_returnsTrue() {
        assertTrue(filter.accept(new File("banana.JPG")))
    }

    @Test
    void testPng_returnsTrue() {
        assertTrue(filter.accept(new File("banana.png")))
    }

    @Test
    void testUppercasePng_returnsTrue() {
        assertTrue(filter.accept(new File("banana.PNG")))
    }

    @Test
    void testJpeg_returnsFalse() {
        assertFalse(filter.accept(new File("banana.jpeg")))
    }

    @Test
    void testGif_returnsFalse() {
        assertFalse(filter.accept(new File("banana.gif")))
    }

    @Test
    void testTiff_returnsFalse() {
        assertFalse(filter.accept(new File("banana.tiff")))
    }

    @Test
    void testBmp_returnsFalse() {
        assertFalse(filter.accept(new File("banana.bmp")))
    }

    @Test
    void testSvg_returnsFalse() {
        assertFalse(filter.accept(new File("banana.svg")))
    }
}
