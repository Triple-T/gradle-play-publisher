package com.github.triplet.gradle.play.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.image
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class InvalidImagesDetectorTest {

    @Test
    fun `Valid icon recognized`() {
        lint()
                .files(image("src/main/play/en-US/listing/icon/icon.png", 512, 512))
                .allowMissingSdk()
                .issues(InvalidImagesDetector.ISSUE)
                .run()
                .expectClean()
    }

    @Test
    fun `Invalid type recognized`() {
        lint()
                .files(image("src/main/play/en-US/listing/icon/icon.gif", 512, 512))
                .allowMissingSdk()
                .issues(InvalidImagesDetector.ISSUE)
                .run()
                .expect("""
                    |src/main/play/en-US/listing/icon/icon.gif: Error: Only PNG and JPG files are allowed [InvalidPlayImages]
                    |1 errors, 0 warnings
                    |""".trimMargin())
    }

    @Test
    fun `Invalid dimension recognized`() {
        lint()
                .files(image("src/main/play/en-US/listing/icon/icon.png", 512, 513))
                .allowMissingSdk()
                .issues(InvalidImagesDetector.ISSUE)
                .run()
                .expect("""
                    |src/main/play/en-US/listing/icon/icon.png: Error: Invalid dimension found [InvalidPlayImages]
                    |1 errors, 0 warnings
                    |""".trimMargin())
    }

}
