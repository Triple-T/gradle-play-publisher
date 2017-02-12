package de.triplet.gradle.play

class PromoteAlphaToProductionTask extends PromoteTask {

    @Override
    String getBaseTrackName() {
        return 'alpha'
    }

    @Override
    String getPromotingTrackName() {
        if (extension.userFraction) {
            return 'rollout'
        } else {
            return 'production'
        }
    }

}