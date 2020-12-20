package com.github.triplet.gradle.play.tasks.internal.workers

internal abstract class EditWorkerBase<T : EditWorkerBase.EditPublishingParams> :
        PlayWorkerBase<T>() {
    protected fun commit() {
        if (config.commit) {
            apiService.scheduleCommit()
        } else {
            apiService.skipCommit()
        }
    }

    internal interface EditPublishingParams : PlayPublishingParams
}
