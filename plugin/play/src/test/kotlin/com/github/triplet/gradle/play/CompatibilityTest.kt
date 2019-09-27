package com.github.triplet.gradle.play

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CompatibilityTest(
        private val gradleVersion: String,
        private val agpVersion: String
) {
    @Test
    fun `Plugin builds successfully`() {
        // language=gradle
        val config = """
            play {
                serviceAccountCredentials = file('some-file.json')
            }
        """

        executeWithVersions(config, gradleVersion, agpVersion, "checkReleaseManifest")
    }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "gradleVersion {0}, agpVersion: {1}")
        fun parameters() = listOf(
                arrayOf("5.6.1", "3.5.0"), // Oldest supported
                arrayOf("5.6.1", "3.5.0"), // Latest stable
                arrayOf("5.6.1", "3.6.0-alpha11") // Latest
        )
    }
}
