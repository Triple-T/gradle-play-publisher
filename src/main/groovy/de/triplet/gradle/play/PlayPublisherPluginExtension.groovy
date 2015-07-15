package de.triplet.gradle.play

class PlayPublisherPluginExtension {

    String serviceAccountEmail

    File pk12File

    boolean uploadImages = false

    boolean errorOnSizeLimit = true

    private String track = 'alpha'

    void setTrack(String track) {
        if (!(track in ['alpha', 'beta', 'rollout', 'production'])) {
            throw new IllegalArgumentException("Track has to be one of 'alpha', 'beta', 'rollout' or 'production'.")
        }

        this.track = track
    }

    def getTrack() {
        return track
    }

    private Double userFraction = 0.1

    void setUserFraction(Double userFraction) {
      this.userFraction = userFraction
    }

    def getUserFraction() {
      return userFraction
    }

}
