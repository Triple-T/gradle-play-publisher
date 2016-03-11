package de.triplet.gradle.play

import org.junit.Test

class PlayRolloutFractionsExtensionTest extends GroovyTestCase {

    @Test
    void test_ShouldReturn001_whenInputIs0005() {
        PlayRolloutFractionsExtension extension = new PlayRolloutFractionsExtension()
        double userFraction = 0.005

        double nextUserFraction = extension.getNextUserFraction(userFraction)

        assertEquals(0.01, nextUserFraction)
    }

    @Test
    void test_ShouldReturn005_whenInputIs001() {
        PlayRolloutFractionsExtension extension = new PlayRolloutFractionsExtension()
        double userFraction = 0.01

        double nextUserFraction = extension.getNextUserFraction(userFraction)

        assertEquals(0.05, nextUserFraction)
    }

    @Test
    void test_ShouldReturn01_whenInputIs005() {
        PlayRolloutFractionsExtension extension = new PlayRolloutFractionsExtension()
        double userFraction = 0.05

        double nextUserFraction = extension.getNextUserFraction(userFraction)

        assertEquals(0.1, nextUserFraction)
    }

    @Test
    void test_ShouldReturn02_whenInputIs01() {
        PlayRolloutFractionsExtension extension = new PlayRolloutFractionsExtension()
        double userFraction = 0.1

        double nextUserFraction = extension.getNextUserFraction(userFraction)

        assertEquals(0.2, nextUserFraction)
    }

    @Test
    void test_ShouldReturn05_whenInputIs02() {
        PlayRolloutFractionsExtension extension = new PlayRolloutFractionsExtension()
        double userFraction = 0.2

        double nextUserFraction = extension.getNextUserFraction(userFraction)

        assertEquals(0.5, nextUserFraction)
    }

    @Test
    void test_ShouldReturn1_whenInputIs05() {
        PlayRolloutFractionsExtension extension = new PlayRolloutFractionsExtension()
        double userFraction = 0.5

        double nextUserFraction = extension.getNextUserFraction(userFraction)

        assertEquals(1, nextUserFraction)
    }

    @Test
    void test_ShouldReturn1_whenInputIs1() {
        PlayRolloutFractionsExtension extension = new PlayRolloutFractionsExtension()
        double userFraction = 1

        double nextUserFraction = extension.getNextUserFraction(userFraction)

        assertEquals(1, nextUserFraction)
    }

}
