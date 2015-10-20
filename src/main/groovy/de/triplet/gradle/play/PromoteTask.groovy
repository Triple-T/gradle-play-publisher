package de.triplet.gradle.play

import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.tasks.TaskAction

abstract class PromoteTask extends PlayPublishTask {

    @TaskAction
    void promoteTask() {
        super.publish()

        Track targetTrack = getTrackByName(getBaseTrackName())
        setUserFractionToTrack(targetTrack)

        edits.tracks()
                .update(variant.applicationId, editId, getPromotingTrackName(), targetTrack)
                .execute()

        Track oldTrack = new Track().setVersionCodes([])

        edits.tracks()
                .update(variant.applicationId, editId, getBaseTrackName(), oldTrack)
                .execute()

        edits.commit(variant.applicationId, editId).execute()
    }

    void setUserFractionToTrack(Track track) {
        if (extension.userFraction) {
            track.setUserFraction(extension.userFraction)
        }
    }

    abstract String getBaseTrackName()

    abstract String getPromotingTrackName()

}