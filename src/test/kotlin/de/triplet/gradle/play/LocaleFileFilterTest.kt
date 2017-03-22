package de.triplet.gradle.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocaleFileFilterTest {
    private val filter = LocaleFileFilter()

    @Test
    fun testLanguage_returnsTrue() {
        assertTrue(filter.accept(File("de")))
    }

    @Test
    fun testLanguageCountry_returnsTrue() {
        assertTrue(filter.accept(File("de-DE")))
    }

    @Test
    fun testSpecialCase1_returnsTrue() {
        assertTrue(filter.accept(File("es-419")))
    }

    @Test
    fun testSpecialCase2_returnsTrue() {
        assertTrue(filter.accept(File("fil")))
    }

    @Test
    fun testUnderscore_returnsFalse() {
        assertFalse(filter.accept(File("de_DE")))
    }

    @Test
    fun testInvalidLength_returnsFalse() {
        assertFalse(filter.accept(File("fil-PH")))
    }
}
