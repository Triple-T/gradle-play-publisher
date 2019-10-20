package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.LocalState
import java.io.File
import javax.inject.Inject

internal abstract class EditTaskBase @Inject constructor(
        extension: PlayPublisherExtension
) : PlayTaskBase(extension) {
    @get:LocalState
    abstract val editIdFile: RegularFileProperty

    companion object {
        fun File.reset() {
            val commitMarker = marked("commit")
            val skippedMarker = marked("skipped")

            check(commitMarker.deleteRecursively()) { "Couldn't delete $commitMarker" }
            check(skippedMarker.deleteRecursively()) { "Couldn't delete $skippedMarker" }
            check(deleteRecursively()) { "Couldn't delete $this" }
        }
    }
}
