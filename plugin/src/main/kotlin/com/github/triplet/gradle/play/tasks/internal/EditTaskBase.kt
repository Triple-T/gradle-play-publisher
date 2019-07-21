package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.marked
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Destroys
import org.gradle.api.tasks.Internal
import java.io.File
import javax.inject.Inject

abstract class EditTaskBase @Inject constructor(
        @get:Internal protected val extension: PlayPublisherExtension
) : DefaultTask() {
    @get:Destroys
    internal abstract val editIdFile: RegularFileProperty

    protected companion object {
        fun File.reset() {
            val commitMarker = marked("commit")
            val skippedMarker = marked("skipped")

            check(commitMarker.deleteRecursively()) { "Couldn't delete $commitMarker" }
            check(skippedMarker.deleteRecursively()) { "Couldn't delete $skippedMarker" }
            check(deleteRecursively()) { "Couldn't delete $this" }
        }
    }
}
