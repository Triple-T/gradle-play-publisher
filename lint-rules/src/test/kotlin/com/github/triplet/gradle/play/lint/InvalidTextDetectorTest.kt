package com.github.triplet.gradle.play.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.source
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class InvalidTextDetectorTest {

    @Test
    fun `Valid text recognized`() {
        lint()
                .files(source("src/main/play/en-US/listing/title", "My App title"))
                .allowMissingSdk()
                .issues(InvalidTextDetector.ISSUE)
                .run()
                .expectClean()
    }

    @Test
    fun `Invalid text recognized`() {
        lint()
                .files(source("src/main/play/en-US/listing/title", "My way too long App that I wish would be accepted but will not"))
                .allowMissingSdk()
                .issues(InvalidTextDetector.ISSUE)
                .run()
                .expect("""
                    |src/main/play/en-US/listing/title: Warning: Text too long [InvalidPlayText]
                    |0 errors, 1 warnings
                    |""".trimMargin())
    }

}
