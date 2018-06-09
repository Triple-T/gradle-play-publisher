package com.github.triplet.gradle.play.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import java.io.IOException
import javax.imageio.ImageIO

class InvalidImagesDetector : PlayDetectorBase() {

    override fun checkImageType(context: Context, imageType: ImageType) {
        if (ImageFileFilter.accept(context.file)) {
            try {
                val image = ImageIO.read(context.file)
                if (image.width < imageType.constraints.minWidth
                        || image.width > imageType.constraints.maxWidth
                        || image.height < imageType.constraints.minHeight
                        || image.height > imageType.constraints.maxHeight) {
                    context.report(
                            ISSUE,
                            Location.create(context.file),
                            "Invalid dimension found"
                    )
                }
            } catch (e: IOException) {
                // TODO: What should we do in this case?!
            }
        } else {
            context.report(
                    ISSUE,
                    Location.create(context.file),
                    "Only PNG and JPG files are allowed"
            )
        }
    }

    companion object {
        val ISSUE = Issue.create(
                "InvalidPlayImages",
                "Images placed in the Play folders does not match the specs",
                "An image that is placed in the Play folder does not match the specs and will result in a failure on upload",
                Category.LINT,
                5,
                Severity.ERROR,
                Implementation(InvalidImagesDetector::class.java, Scope.ALL)
        ).setEnabledByDefault(true)
    }

}
