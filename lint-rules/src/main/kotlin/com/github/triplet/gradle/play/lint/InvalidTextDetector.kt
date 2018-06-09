package com.github.triplet.gradle.play.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

class InvalidTextDetector : PlayDetectorBase() {

    override fun checkListingDetail(context: Context, listingDetail: ListingDetail) {
        val textLength = context.file.readText().length
        if (textLength > listingDetail.maxLength) {
            context.report(
                    ISSUE,
                    Location.create(context.file),
                    "Text too long"
            )

        }
    }

    companion object {
        val ISSUE = Issue.create(
                "InvalidPlayText",
                "TODO",
                "TODO",
                Category.LINT,
                5,
                Severity.WARNING,
                Implementation(InvalidTextDetector::class.java, Scope.ALL)
        ).setEnabledByDefault(true)
    }

}
