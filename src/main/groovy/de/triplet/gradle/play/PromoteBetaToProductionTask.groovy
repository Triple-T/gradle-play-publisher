package de.triplet.gradle.play

class PromoteBetaToProductionTask extends PromoteTask {

    @Override
    String getBaseTrackName() {
        return 'beta'
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