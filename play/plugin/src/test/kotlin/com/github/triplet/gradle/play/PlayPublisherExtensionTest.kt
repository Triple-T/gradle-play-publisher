package com.github.triplet.gradle.play

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.play.internal.config
import com.github.triplet.gradle.play.internal.evaluate
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

        assertThat(merged).isSameInstanceAs(ext)
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
        assertThat(merged.config.retainMainObb).isEqualTo(8)
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

        assertThat(merged).isSameInstanceAs(p0)
    }

    @Test
    fun `Merging extensions with multiple returns superset`() {
        val p0 = PlayPublisherExtension().apply { track = "test" }
        val p1 = PlayPublisherExtension().apply { releaseStatus = "inProgress" }
        val exts = listOf(p0, p1)

        val merged = mergeExtensions(exts)
        p1.evaluate()

        assertThat(merged.config.track).isEqualTo("test")
        assertThat(merged.config.releaseStatus).isEqualTo(ReleaseStatus.IN_PROGRESS)
    }

    @Test
    fun `Merging extensions with multiple nested returns superset`() {
        val p0 = PlayPublisherExtension().apply { track = "test" }
        val p1 = PlayPublisherExtension().apply { retain.mainObb = 8 }
        val exts = listOf(p0, p1)

        val merged = mergeExtensions(exts)
        p1.evaluate()

        assertThat(merged.config.track).isEqualTo("test")
        assertThat(merged.config.retainMainObb).isEqualTo(8)
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
        p4.evaluate()

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
        p3.evaluate()

        assertThat(merged.config.track).isEqualTo("test")
        assertThat(merged.config.retainMainObb).isEqualTo(8)
        assertThat(merged.config.retainPatchObb).isEqualTo(16)
    }

    @Test
    fun `Merging extension assigns children correctly`() {
        val p0 = PlayPublisherExtension().apply { track = "a" }
        val p1 = PlayPublisherExtension().apply { track = "b" }
        val p1dash1 = PlayPublisherExtension().apply { track = "c" }
        val p2 = PlayPublisherExtension().apply { track = "d" }
        val exts1 = listOf(p0, p1, p2)
        val exts2 = listOf(p0, p1dash1)

        mergeExtensions(exts1)
        mergeExtensions(exts2)

        assertThat(p2._children).containsExactly(p1)
        assertThat(p1._children).containsExactly(p0)
        assertThat(p1dash1._children).containsExactly(p0)
        assertThat(p0._children).isEmpty()
    }

    @Test
    fun `Updating parent extension propagates changes`() {
        val p0 = PlayPublisherExtension().apply { releaseName = "Hello" }
        val p1 = PlayPublisherExtension().apply { fromTrack = "test" }
        val p2 = PlayPublisherExtension().apply { track = "root" }
        val exts = listOf(p0, p1, p2)

        mergeExtensions(exts)

        p2.track = "new"

        assertThat(p2.track).isEqualTo("new")
        assertThat(p1.track).isEqualTo("new")
        assertThat(p0.track).isEqualTo("new")
    }

    @Test
    fun `Updating parent extension doesn't overwrite existing properties`() {
        val p0 = PlayPublisherExtension().apply { track = "child" }
        val p1 = PlayPublisherExtension().apply { track = "parent" }
        val exts = listOf(p0, p1)

        mergeExtensions(exts)

        p1.track = "new parent"

        assertThat(p1.track).isEqualTo("new parent")
        assertThat(p0.track).isEqualTo("child")
    }
}
