package com.github.triplet.gradle.common.validation

import com.android.build.api.AndroidPluginVersion
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RuntimeValidatorTest {
    @Test
    fun `Gradle version below minimum throws`() {
        val validator = GradleRuntimeValidator(
                currentGradleVersion = GradleVersion.version("0.0.0"),
                minGradleVersion = GradleVersion.version("1.0.0"),
        )

        assertThrows<IllegalStateException> { validator.validate() }
    }

    @Test
    fun `Gradle version at minimum succeeds`() {
        val validator = GradleRuntimeValidator(
                currentGradleVersion = GradleVersion.version("1.0.0"),
                minGradleVersion = GradleVersion.version("1.0.0"),
        )

        validator.validate()
    }

    @Test
    fun `Gradle version above minimum succeeds`() {
        val validator = GradleRuntimeValidator(
                currentGradleVersion = GradleVersion.version("2.0.0"),
                minGradleVersion = GradleVersion.version("1.0.0"),
        )

        validator.validate()
    }

    @Test
    fun `Agp version below minimum throws`() {
        val validator = AgpRuntimeValidator(
                currentAgpVersion = AndroidPluginVersion(0, 0),
                minAgpVersion = AndroidPluginVersion(1, 0),
        )

        assertThrows<IllegalStateException> { validator.validate() }
    }

    @Test
    fun `Agp version at minimum succeeds`() {
        val validator = AgpRuntimeValidator(
                currentAgpVersion = AndroidPluginVersion(1, 0),
                minAgpVersion = AndroidPluginVersion(1, 0),
        )

        validator.validate()
    }

    @Test
    fun `Agp version above minimum succeeds`() {
        val validator = AgpRuntimeValidator(
                currentAgpVersion = AndroidPluginVersion(2, 0),
                minAgpVersion = AndroidPluginVersion(1, 0),
        )

        validator.validate()
    }
}
