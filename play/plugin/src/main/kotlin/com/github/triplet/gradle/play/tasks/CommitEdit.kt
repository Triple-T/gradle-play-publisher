package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

internal abstract class CommitEdit @Inject constructor(
        extension: PlayPublisherExtension,
) : PublishTaskBase(extension) {
    @TaskAction
    fun commit() {
        if (project.gradle.taskGraph.allTasks.any { it.state.failure != null }) {
            logger.info("Build failed, skipping")
            apiService.get().cleanup()
            return
        }

        project.serviceOf<WorkerExecutor>().noIsolation().submit(Committer::class) {
            paramsForBase(this)
        }
    }

    abstract class Committer : PlayWorkerBase<Committer.Params>() {
        override fun execute() {
            if (apiService.shouldCommit()) {
                println("Committing changes")
                try {
                    apiService.commit()
                } finally {
                    apiService.cleanup()
                }
            } else if (apiService.shouldSkip()) {
                println("Changes pending commit")
                try {
                    apiService.validate()
                } catch (e: Exception) {
                    apiService.cleanup()
                    throw e
                }
            } else {
                Logging.getLogger(CommitEdit::class.java).info("Nothing to commit, skipping")
            }
        }

        interface Params : PlayPublishingParams
    }
}
