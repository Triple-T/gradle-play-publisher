package de.triplet.gradle.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ImageFileFilterTest {
    private val filter = ImageFileFilter()

    @Test
    fun testJpg_returnsTrue() {
        assertTrue(filter.accept(File("banana.jpg")))
    }

    @Test
    fun testUppercaseJpg_returnsTrue() {
        assertTrue(filter.accept(File("banana.JPG")))
    }

    @Test
    fun testPng_returnsTrue() {
        assertTrue(filter.accept(File("banana.png")))
    }

    @Test
    fun testUppercasePng_returnsTrue() {
        assertTrue(filter.accept(File("banana.PNG")))
    }

    @Test
    fun testJpeg_returnsFalse() {
        assertFalse(filter.accept(File("banana.jpeg")))
    }

    @Test
    fun testGif_returnsFalse() {
        assertFalse(filter.accept(File("banana.gif")))
    }

    @Test
    fun testTiff_returnsFalse() {
        assertFalse(filter.accept(File("banana.tiff")))
    }

    @Test
    fun testBmp_returnsFalse() {
        assertFalse(filter.accept(File("banana.bmp")))
    }

    @Test
    fun testSvg_returnsFalse() {
        assertFalse(filter.accept(File("banana.svg")))
    }
}
