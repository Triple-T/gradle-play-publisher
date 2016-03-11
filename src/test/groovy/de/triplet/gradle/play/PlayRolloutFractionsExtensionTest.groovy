package de.triplet.gradle.play

import org.junit.Test

class PlayRolloutFractionsExtensionTest extends GroovyTestCase {

    @Test
    void test_ShouldReturn001_whenInputIs0005() {
        PlayRolloutFractionsExtension extension = new PlayRolloutFractionsExtension();
        double userFraction = 0.005;

        double nextUserFraction = extension.getNextUserFraction(userFraction);

        assertEquals(0.01, nextUserFraction);
    }
}
