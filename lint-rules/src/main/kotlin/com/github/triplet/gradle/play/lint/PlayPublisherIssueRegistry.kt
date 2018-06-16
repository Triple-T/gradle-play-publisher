package com.github.triplet.gradle.play.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class PlayPublisherIssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(
                InvalidImagesDetector.ISSUE,
                InvalidTextDetector.ISSUE
        )

    override val api: Int
        get() = CURRENT_API
}
