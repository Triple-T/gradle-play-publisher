package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.marked
import com.github.triplet.gradle.play.tasks.internal.EditTaskBase
import com.github.triplet.gradle.play.tasks.internal.buildPublisher
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class CommitEdit @Inject constructor(
        extension: PlayPublisherExtension
) : EditTaskBase(extension) {
    @TaskAction
    fun commit() {
        val file = editIdFile.asFile.get()

        if (project.gradle.taskGraph.allTasks.any { it.state.failure != null }) {
            println("Build failed, skipping")
            file.reset()
            return
        }

        project.serviceOf<WorkerExecutor>().noIsolation().submit(Committer::class) {
            config.set(extension.serializableConfig)
            editIdFile.set(file)
        }
    }

    internal abstract class Committer : WorkAction<Committer.Params> {
        override fun execute() {
            val file = parameters.editIdFile.get()
            if (file.marked("commit").exists()) {
                println("Committing changes")
                val appId = file.nameWithoutExtension
                try {
                    parameters.config.get().buildPublisher()
                            .edits().commit(appId, file.readText()).execute()
                } finally {
                    file.reset()
                }
            } else if (file.marked("skipped").exists()) {
                println("Changes pending commit")
            } else {
                println("Nothing to commit, skipping")
            }
        }

        interface Params : WorkParameters {
            val config: Property<PlayPublisherExtension.Config>
            val editIdFile: Property<File>
        }
    }
}
