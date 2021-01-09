package com.github.triplet.gradle.play.helpers

import org.gradle.testkit.runner.BuildResult
import java.io.File

interface IntegrationTest {
    val appDir: File
    val playgroundDir: File

    fun File.escaped(): String

    fun execute(config: String, vararg tasks: String): BuildResult

    fun executeExpectingFailure(config: String, vararg tasks: String): BuildResult

    fun String.withAndroidBlock() = """
        android {
            $this
        }
    """.trimIndent()
}
