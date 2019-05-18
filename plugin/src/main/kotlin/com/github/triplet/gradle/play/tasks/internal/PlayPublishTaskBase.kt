package com.github.triplet.gradle.play.tasks.internal

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.EDIT_ID_FILE
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import java.io.File

abstract class PlayPublishTaskBase(
        @get:Nested internal open val extension: PlayPublisherExtension,
        @get:Internal internal val variant: ApplicationVariant
) : DefaultTask() {
    @get:Internal internal val savedEditId = File(project.rootProject.buildDir, EDIT_ID_FILE)

    @get:Internal protected val publisher by lazy { extension.buildPublisher() }

    @Internal
    protected fun getOrCreateEditId(): String {
        return publisher.getOrCreateEditId(variant.applicationId, savedEditId)
    }

    protected fun commit(editId: String) {
        publisher.commit(extension, variant.applicationId, editId, savedEditId)
    }
}
