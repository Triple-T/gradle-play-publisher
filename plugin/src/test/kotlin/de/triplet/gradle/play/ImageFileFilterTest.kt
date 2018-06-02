package de.triplet.gradle.play

import de.triplet.gradle.play.internal.ImageFileFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ImageFileFilterTest {
    @Test
    fun `'jpg' is valid`() {
        assertTrue(ImageFileFilter.accept(File("banana.jpg")))
    }

    @Test
    fun `'JPG' is valid`() {
        assertTrue(ImageFileFilter.accept(File("banana.JPG")))
    }

    @Test
    fun `'png' is valid`() {
        assertTrue(ImageFileFilter.accept(File("banana.png")))
    }

    @Test
    fun `'PNG' is valid`() {
        assertTrue(ImageFileFilter.accept(File("banana.PNG")))
    }

    @Test
    fun `'jpeg' is invalid`() {
        assertFalse(ImageFileFilter.accept(File("banana.jpeg")))
    }

    @Test
    fun `'gif' is invalid`() {
        assertFalse(ImageFileFilter.accept(File("banana.gif")))
    }

    @Test
    fun `'tiff' is invalid`() {
        assertFalse(ImageFileFilter.accept(File("banana.tiff")))
    }

    @Test
    fun `'bmp' is invalid`() {
        assertFalse(ImageFileFilter.accept(File("banana.bmp")))
    }

    @Test
    fun `'svg' is invalid`() {
        assertFalse(ImageFileFilter.accept(File("banana.svg")))
    }
}
