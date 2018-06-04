package com.github.triplet.gradle.play.internal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocaleFileFilterTest {
    @Test
    fun `Language is valid`() {
        assertTrue(LocaleFileFilter.accept(File("de")))
    }

    @Test
    fun `Language + country are valid`() {
        assertTrue(LocaleFileFilter.accept(File("de-DE")))
    }

    @Test
    fun `'es' special case is valid`() {
        assertTrue(LocaleFileFilter.accept(File("es-419")))
    }

    @Test
    fun `'file' special case is valid`() {
        assertTrue(LocaleFileFilter.accept(File("fil")))
    }

    @Test
    fun `Underscore is invalid`() {
        assertFalse(LocaleFileFilter.accept(File("de_DE")))
    }

    @Test
    fun `Too long is invalid`() {
        assertFalse(LocaleFileFilter.accept(File("fil-PH")))
    }

    @Test
    fun `Too short is invalid`() {
        assertFalse(LocaleFileFilter.accept(File("a")))
    }
}
