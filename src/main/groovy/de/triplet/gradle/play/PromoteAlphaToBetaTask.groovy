package de.triplet.gradle.play

import com.google.api.services.androidpublisher.model.Track

class PromoteAlphaToBetaTask extends PromoteTask {

    @Override
    String getBaseTrackName() {
        return 'alpha'
    }

    @Override
    String getPromotingTrackName() {
        return 'beta'
    }

    @Override
    void setUserFractionToTrack(Track track) {
    }

}