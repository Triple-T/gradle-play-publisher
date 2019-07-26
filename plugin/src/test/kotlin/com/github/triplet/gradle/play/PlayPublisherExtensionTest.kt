package com.github.triplet.gradle.play

import com.github.triplet.gradle.play.internal.ReleaseStatus
import com.github.triplet.gradle.play.internal.ResolutionStrategy
import com.github.triplet.gradle.play.internal.mergeExtensions
import com.github.triplet.gradle.play.internal.mergeWith
import org.junit.Test
import kotlin.test.assertFails

class PlayPublisherExtensionTest {
    @Test
    fun `Config does not share object instance with extension`() {
        val ext = PlayPublisherExtension().apply { track = "test" }
        val copy = ext.config

        copy.track = "other"

        assert(ext.track == "test")
    }

    @Test
    fun `Merging extension with null returns self`() {
        val ext = PlayPublisherExtension().apply { track = "test" }

        val merged = ext.mergeWith(null)

        assert(ext === merged)
    }

    @Test
    fun `Merging extension with same doesn't change anything`() {
        val ext = PlayPublisherExtension().apply { track = "test" }
        val other = PlayPublisherExtension().apply { track = "test" }

        val merged = ext.mergeWith(other)

        assert(ext.config == merged.config)
    }

    @Test
    fun `Merging extension with other returns superset`() {
        val ext = PlayPublisherExtension().apply { track = "test" }
        val other = PlayPublisherExtension().apply { releaseStatus = "inProgress" }

        val merged = ext.mergeWith(other)

        assert(merged.config.track == "test")
        assert(merged.config.releaseStatus == ReleaseStatus.IN_PROGRESS)
    }

    @Test
    fun `Merging extensions with empty throws`() {
        val exts = emptyList<PlayPublisherExtension>()

        assertFails { mergeExtensions(exts) }
    }

    @Test
    fun `Merging extensions with single returns self`() {
        val p0 = PlayPublisherExtension().apply { track = "test" }
        val exts = listOf(p0)

        val merged = mergeExtensions(exts)

        assert(merged === p0)
    }

    @Test
    fun `Merging extensions with multiple returns superset`() {
        val p0 = PlayPublisherExtension().apply { track = "test" }
        val p1 = PlayPublisherExtension().apply { releaseStatus = "inProgress" }
        val exts = listOf(p0, p1)

        val merged = mergeExtensions(exts)

        assert(merged.config.track == "test")
        assert(merged.config.releaseStatus == ReleaseStatus.IN_PROGRESS)
    }

    @Test
    fun `Merging extensions with multiple overlapping returns prioritized superset`() {
        val p0 = PlayPublisherExtension().apply { track = "test" }
        val p1 = PlayPublisherExtension().apply {
            track = "other"
            releaseStatus = "inProgress"
        }
        val p3 = PlayPublisherExtension().apply {
            releaseStatus = "completed"
            userFraction = 0.5
        }
        val p4 = PlayPublisherExtension().apply {
            track = "other2"
            defaultToAppBundles = true
            resolutionStrategy = "ignore"
        }
        val exts = listOf(p0, p1, p3, p4)

        val merged = mergeExtensions(exts)

        assert(merged.config.track == "test")
        assert(merged.config.releaseStatus == ReleaseStatus.IN_PROGRESS)
        assert(merged.config.userFraction == 0.5)
        assert(merged.config.defaultToAppBundles == true)
        assert(merged.config.resolutionStrategy == ResolutionStrategy.IGNORE)
    }
}
