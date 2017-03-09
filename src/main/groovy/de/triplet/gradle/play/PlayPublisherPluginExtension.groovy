package de.triplet.gradle.play

class PlayPublisherPluginExtension {

    String serviceAccountEmail

    File pk12File

    File jsonFile

    boolean uploadImages = false

    boolean errorOnSizeLimit = true

    private String track = 'alpha'

    boolean untrackOld = false

    void setTrack(String track) {
        if (!(track in ['alpha', 'beta', 'rollout', 'production'])) {
            throw new IllegalArgumentException('Track has to be one of \'alpha\', \'beta\', \'rollout\' or \'production\'.')
        }

        this.track = track
    }

    def getTrack() {
        return track
    }

    Double userFraction = 0.1

}
