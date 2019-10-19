package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.helpers.FIXTURES_DIR
import com.github.triplet.gradle.play.helpers.FIXTURE_WORKING_DIR
import com.github.triplet.gradle.play.helpers.execute
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class IntegrationTestBase {
    @get:Rule
    val tempDir = TemporaryFolder()

    @Before
    fun cleanDirs() {
        execute("", "clean")
        assertThat(File(FIXTURE_WORKING_DIR, "build").exists()).isFalse()
    }

    @Before
    fun copyResDirs() {
        File(FIXTURES_DIR, javaClass.simpleName).copyRecursively(FIXTURE_WORKING_DIR)
    }

    @After
    fun cleanupResDirs() {
        for (file in File(FIXTURES_DIR, javaClass.simpleName).listFiles().orEmpty()) {
            File(FIXTURE_WORKING_DIR, file.name).deleteRecursively()
        }
    }

    protected fun escapedTempDir() = tempDir.root.toString().replace("\\", "\\\\")
}
