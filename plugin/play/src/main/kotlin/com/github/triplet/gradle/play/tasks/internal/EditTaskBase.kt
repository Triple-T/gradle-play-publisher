package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.marked
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.LocalState
import java.io.File
import javax.inject.Inject

abstract class EditTaskBase @Inject constructor(
        extension: PlayPublisherExtension
) : PlayTaskBase(extension) {
    @get:LocalState
    internal abstract val editIdFile: RegularFileProperty

    internal companion object {
        fun File.reset() {
            val commitMarker = marked("commit")
            val skippedMarker = marked("skipped")

            check(commitMarker.deleteRecursively()) { "Couldn't delete $commitMarker" }
            check(skippedMarker.deleteRecursively()) { "Couldn't delete $skippedMarker" }
            check(deleteRecursively()) { "Couldn't delete $this" }
        }
    }
}
