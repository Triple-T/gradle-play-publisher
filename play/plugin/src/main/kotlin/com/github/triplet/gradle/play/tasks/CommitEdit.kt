package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.PublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.workers.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.workers.paramsForBase
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@Suppress("LeakingThis")
@DisableCachingByDefault
internal abstract class CommitEdit @Inject constructor(
        private val gradle: Gradle,
        extension: PlayPublisherExtension,
        private val executor: WorkerExecutor,
) : PublishTaskBase(extension) {
    init {
        onlyIf {
            val buildFailed = gradle.taskGraph.allTasks.any { it.state.failure != null }
            if (buildFailed) apiService.get().cleanup()
            !buildFailed
        }
    }

    @TaskAction
    fun commit() {
        executor.noIsolation().submit(Committer::class) {
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
