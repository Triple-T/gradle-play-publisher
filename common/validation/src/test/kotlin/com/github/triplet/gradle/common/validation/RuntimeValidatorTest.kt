package com.github.triplet.gradle.common.validation

import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RuntimeValidatorTest {
    @Test
    fun `Gradle version below minimum throws`() {
        val validator = newValidator(
                currentGradle = GradleVersion.version("0.0.0"),
                minGradle = GradleVersion.version("1.0.0")
        )

        assertThrows<IllegalStateException> { validator.validate() }
    }

    @Test
    fun `Gradle version at minimum succeeds`() {
        val validator = newValidator(
                currentGradle = GradleVersion.version("1.0.0"),
                minGradle = GradleVersion.version("1.0.0")
        )

        validator.validate()
    }

    @Test
    fun `Gradle version above minimum succeeds`() {
        val validator = newValidator(
                currentGradle = GradleVersion.version("2.0.0"),
                minGradle = GradleVersion.version("1.0.0")
        )

        validator.validate()
    }

    @Test
    fun `Agp version below minimum throws`() {
        val validator = newValidator(
                currentAgp = VersionNumber.parse("0.0.0"),
                minAgp = VersionNumber.parse("1.0.0")
        )

        assertThrows<IllegalStateException> { validator.validate() }
    }

    @Test
    fun `Agp version at minimum succeeds`() {
        val validator = newValidator(
                currentAgp = VersionNumber.parse("1.0.0"),
                minAgp = VersionNumber.parse("1.0.0")
        )

        validator.validate()
    }

    @Test
    fun `Agp version above minimum succeeds`() {
        val validator = newValidator(
                currentAgp = VersionNumber.parse("2.0.0"),
                minAgp = VersionNumber.parse("1.0.0")
        )

        validator.validate()
    }

    private fun newValidator(
            currentGradle: GradleVersion = GradleVersion.version("0.0.0"),
            minGradle: GradleVersion = GradleVersion.version("0.0.0"),
            currentAgp: VersionNumber = VersionNumber.parse("0.0.0"),
            minAgp: VersionNumber = VersionNumber.parse("0.0.0")
    ) = RuntimeValidator(currentGradle, minGradle, currentAgp, minAgp)
}
