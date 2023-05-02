package com.github.triplet.gradle.play.helpers

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

interface IntegrationTest {
    val appDir: File
    val playgroundDir: File

    val factoryInstallerStatement: String

    val withConfigurationCache: Boolean

    fun File.escaped(): String

    fun execute(config: String, vararg tasks: String): BuildResult

    fun executeExpectingFailure(config: String, vararg tasks: String): BuildResult

    fun executeGradle(
            expectFailure: Boolean,
            block: GradleRunner.() -> Unit,
    ): BuildResult

    fun String.withAndroidBlock() = """
        android {
            $this
        }
    """.trimIndent()
}
