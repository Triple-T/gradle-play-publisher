package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.marked
import com.github.triplet.gradle.play.tasks.internal.EditTaskBase
import com.github.triplet.gradle.play.tasks.internal.buildPublisher
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
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

        project.serviceOf<WorkerExecutor>().submit(Committer::class) {
            params(Committer.Params(extension.serializableConfig, file))
        }
    }

    private class Committer @Inject constructor(private val p: Params) : Runnable {
        override fun run() {
            val file = p.editIdFile
            if (file.marked("commit").exists()) {
                println("Committing changes")
                val appId = file.nameWithoutExtension
                try {
                    p.config.buildPublisher().edits().commit(appId, file.readText()).execute()
                } finally {
                    file.reset()
                }
            } else if (file.marked("skipped").exists()) {
                println("Changes pending commit")
            } else {
                println("Nothing to commit, skipping")
            }
        }

        data class Params(
                val config: PlayPublisherExtension.Config,
                val editIdFile: File
        ) : Serializable
    }
}
