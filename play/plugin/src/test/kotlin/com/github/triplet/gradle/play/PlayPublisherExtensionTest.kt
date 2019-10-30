package com.github.triplet.gradle.play

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.play.internal.mergeExtensions
import com.github.triplet.gradle.play.internal.mergeWith
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class PlayPublisherExtensionTest {
    @Test
    fun `Config does not share object instance with extension`() {
        val ext = PlayPublisherExtension().apply { track = "test" }
        val copy = ext.config

        copy.track = "other"

        assertThat(ext.track).isEqualTo("test")
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

        assertThat(ext.config).isEqualTo(merged.config)
    }

    @Test
    fun `Merging extension with other returns superset`() {
        val ext = PlayPublisherExtension().apply { track = "test" }
        val other = PlayPublisherExtension().apply { releaseStatus = "inProgress" }

        val merged = ext.mergeWith(other)

        assertThat(merged.config.track).isEqualTo("test")
        assertThat(merged.config.releaseStatus).isEqualTo(ReleaseStatus.IN_PROGRESS)
    }

    @Test
    fun `Merging extension with other nested returns superset`() {
        val ext = PlayPublisherExtension().apply { track = "test" }
        val other = PlayPublisherExtension().apply { retain.mainObb = 8 }

        val merged = ext.mergeWith(other)

        assertThat(merged.config.track).isEqualTo("test")
        assertThat(merged.config.retain.mainObb).isEqualTo(8)
    }

    @Test
    fun `Merging extensions with empty throws`() {
        val exts = emptyList<PlayPublisherExtension>()

        assertThrows(NoSuchElementException::class.java) { mergeExtensions(exts) }
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

        assertThat(merged.config.track).isEqualTo("test")
        assertThat(merged.config.releaseStatus).isEqualTo(ReleaseStatus.IN_PROGRESS)
    }

    @Test
    fun `Merging extensions with multiple nested returns superset`() {
        val p0 = PlayPublisherExtension().apply { track = "test" }
        val p1 = PlayPublisherExtension().apply { retain.mainObb = 8 }
        val exts = listOf(p0, p1)

        val merged = mergeExtensions(exts)

        assertThat(merged.config.track).isEqualTo("test")
        assertThat(merged.config.retain.mainObb).isEqualTo(8)
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

        assertThat(merged.config.track).isEqualTo("test")
        assertThat(merged.config.releaseStatus).isEqualTo(ReleaseStatus.IN_PROGRESS)
        assertThat(merged.config.userFraction).isEqualTo(0.5)
        assertThat(merged.config.defaultToAppBundles).isEqualTo(true)
        assertThat(merged.config.resolutionStrategy).isEqualTo(ResolutionStrategy.IGNORE)
    }

    @Test
    fun `Merging extensions with multiple nested overlapping returns prioritized superset`() {
        val p0 = PlayPublisherExtension().apply { track = "test" }
        val p1 = PlayPublisherExtension().apply { retain.mainObb = 8 }
        val p3 = PlayPublisherExtension().apply {
            retain.mainObb = 16
            retain.patchObb = 16
        }
        val exts = listOf(p0, p1, p3)

        val merged = mergeExtensions(exts)

        assertThat(merged.config.track).isEqualTo("test")
        assertThat(merged.config.retain.mainObb).isEqualTo(8)
        assertThat(merged.config.retain.patchObb).isEqualTo(16)
    }
}
