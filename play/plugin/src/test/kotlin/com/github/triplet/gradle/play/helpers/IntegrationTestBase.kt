package com.github.triplet.gradle.play.helpers

import com.github.triplet.gradle.common.utils.orNull
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
    fun resetOutputs() {
        execute("", "clean")
        assertThat(File(FIXTURE_WORKING_DIR, "build").exists()).isFalse()
    }

    @Before
    fun initTestResources() {
        File(FIXTURES_DIR, javaClass.simpleName).orNull()?.copyRecursively(FIXTURE_WORKING_DIR)
    }

    @After
    fun cleanupTestResources() {
        for (file in File(FIXTURES_DIR, javaClass.simpleName).listFiles().orEmpty()) {
            File(FIXTURE_WORKING_DIR, file.name).deleteRecursively()
        }
    }

    protected fun escapedTempDir() = tempDir.root.toString().replace("\\", "\\\\")
}
