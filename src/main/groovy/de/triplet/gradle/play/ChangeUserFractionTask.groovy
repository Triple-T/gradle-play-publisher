package de.triplet.gradle.play

import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.tasks.TaskAction

class ChangeUserFractionTask extends PlayPublishTask {
    @TaskAction
    void changeUserFraction() {
        super.publish()

        Track targetTrack = getTrackByName('rollout')

        if (extension.userFraction) {
            targetTrack.setUserFraction(extension.userFraction)
        }

        edits.tracks()
                .update(variant.applicationId, editId, 'rollout', targetTrack)
                .execute()

        edits.commit(variant.applicationId, editId).execute()
    }
}