package com.github.triplet.gradle.play

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.play.internal.ExtensionMergeHolder
import com.github.triplet.gradle.play.internal.mergeExtensions
import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PlayPublisherExtensionTest {
    private lateinit var project: Project

    @BeforeEach
    fun `Initialize Gradle project`(@TempDir tempDir: File) {
        project = ProjectBuilder.builder().withProjectDir(tempDir).build()
    }

    @Test
    fun `Merging extensions with empty throws`() {
        val exts = emptyList<PlayPublisherExtension>()

        assertThrows<NoSuchElementException> { mergeExtensions(exts.withCopies()) }
    }

    @Test
    fun `Merging extensions with single returns self`() {
        val p0 = Extension().apply { track.set("test") }
        val exts = listOf(p0)

        val merged = mergeExtensions(exts.withCopies())

        assertThat(merged).isSameInstanceAs(p0)
    }

    @Test
    fun `Merging extensions with multiple returns superset`() {
        val p0 = Extension().apply { track.set("test") }
        val p1 = Extension().apply { releaseStatus.set(ReleaseStatus.IN_PROGRESS) }
        val exts = listOf(p0, p1)

        val merged = mergeExtensions(exts.withCopies())

        assertThat(merged.track.get()).isEqualTo("test")
        assertThat(merged.releaseStatus.get()).isEqualTo(ReleaseStatus.IN_PROGRESS)
    }

    @Test
    fun `Merging extensions with multiple nested returns superset`() {
        val p0 = Extension().apply { track.set("test") }
        val p1 = Extension().apply { retain.mainObb.set(8) }
        val exts = listOf(p0, p1)

        val merged = mergeExtensions(exts.withCopies())

        assertThat(merged.track.get()).isEqualTo("test")
        assertThat(merged.retain.mainObb.get()).isEqualTo(8)
    }

    @Test
    fun `Merging extensions with multiple overlapping returns prioritized superset`() {
        val p0 = Extension().apply { track.set("test") }
        val p1 = Extension().apply {
            track.set("other")
            releaseStatus.set(ReleaseStatus.IN_PROGRESS)
        }
        val p3 = Extension().apply {
            releaseStatus.set(ReleaseStatus.COMPLETED)
            userFraction.set(0.5)
        }
        val p4 = Extension().apply {
            track.set("other2")
            defaultToAppBundles.set(true)
            resolutionStrategy.set(ResolutionStrategy.IGNORE)
        }
        val exts = listOf(p0, p1, p3, p4)

        val merged = mergeExtensions(exts.withCopies())

        assertThat(merged.track.get()).isEqualTo("test")
        assertThat(merged.releaseStatus.get()).isEqualTo(ReleaseStatus.IN_PROGRESS)
        assertThat(merged.userFraction.get()).isEqualTo(0.5)
        assertThat(merged.defaultToAppBundles.get()).isEqualTo(true)
        assertThat(merged.resolutionStrategy.get()).isEqualTo(ResolutionStrategy.IGNORE)
    }

    @Test
    fun `Merging extensions with multiple nested overlapping returns prioritized superset`() {
        val p0 = Extension().apply { track.set("test") }
        val p1 = Extension().apply { retain.mainObb.set(8) }
        val p3 = Extension().apply {
            retain.mainObb.set(16)
            retain.patchObb.set(16)
        }
        val exts = listOf(p0, p1, p3)

        val merged = mergeExtensions(exts.withCopies())

        assertThat(merged.track.get()).isEqualTo("test")
        assertThat(merged.retain.mainObb.get()).isEqualTo(8)
        assertThat(merged.retain.patchObb.get()).isEqualTo(16)
    }

    @Test
    fun `Merging extensions with list properties works`() {
        val p0 = Extension()
        val p1 = Extension().apply { retain.artifacts.set(listOf(1, 2, 3)) }
        val exts = listOf(p0, p1)

        val merged = mergeExtensions(exts.withCopies())

        assertThat(merged.retain.artifacts.get()).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `Updating parent extension propagates changes`() {
        val p0 = Extension().apply { releaseName.set("Hello") }
        val p1 = Extension().apply { fromTrack.set("test") }
        val p2 = Extension().apply { track.set("root") }
        val exts = listOf(p0, p1, p2)

        val merged = mergeExtensions(exts.withCopies())

        p2.track.set("new")

        assertThat(merged.track.get()).isEqualTo("new")
    }

    @Test
    fun `Updating parent extension doesn't overwrite existing properties`() {
        val p0 = Extension().apply { track.set("child") }
        val p1 = Extension().apply { track.set("parent") }
        val exts = listOf(p0, p1)

        mergeExtensions(exts.withCopies())

        p1.track.set("new parent")

        assertThat(p1.track.get()).isEqualTo("new parent")
        assertThat(p0.track.get()).isEqualTo("child")
    }

    private fun List<PlayPublisherExtension>.withCopies() =
            map { ExtensionMergeHolder(it, Extension()) }

    private inner class Extension : PlayPublisherExtension("test") {
        override val versionCode: Property<Long> get() = project.objects.property()
        override val enabled: Property<Boolean> = project.objects.property()
        override val serviceAccountCredentials: RegularFileProperty = project.objects.fileProperty()
        override val useApplicationDefaultCredentials: Property<Boolean> = project.objects.property()
        override val impersonateServiceAccount: Property<String> = project.objects.property()
        override val defaultToAppBundles: Property<Boolean> = project.objects.property()
        override val commit: Property<Boolean> = project.objects.property()
        override val fromTrack: Property<String> = project.objects.property()
        override val track: Property<String> = project.objects.property()
        override val promoteTrack: Property<String> = project.objects.property()
        override val userFraction: Property<Double> = project.objects.property()
        override val updatePriority: Property<Int> = project.objects.property()
        override val releaseStatus: Property<ReleaseStatus> = project.objects.property()
        override val releaseName: Property<String> = project.objects.property()
        override val resolutionStrategy: Property<ResolutionStrategy> = project.objects.property()
        override val artifactDir: DirectoryProperty = project.objects.directoryProperty()
        override val retain = object : Retain() {
            override val artifacts: ListProperty<Long> = project.objects.listProperty()
            override val mainObb: Property<Int> = project.objects.property()
            override val patchObb: Property<Int> = project.objects.property()
        }
    }
}
