package com.github.triplet.gradle.play.tasks.internal

import com.github.triplet.gradle.common.utils.marked
import com.github.triplet.gradle.play.PlayPublisherExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import java.io.File
import javax.inject.Inject

internal abstract class EditTaskBase @Inject constructor(
        extension: PlayPublisherExtension
) : PlayTaskBase(extension) {
    @get:Internal
    abstract val editIdFile: RegularFileProperty

    companion object {
        val File.editIdAndFriends
            get() = listOf(this, marked("commit"), marked("skipped"))
    }
}
