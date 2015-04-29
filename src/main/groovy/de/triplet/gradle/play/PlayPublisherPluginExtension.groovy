package de.triplet.gradle.play

class PlayPublisherPluginExtension {

    String serviceAccountEmail

    File pk12File

    boolean uploadImages = false

    boolean errorOnSizeLimit = true;

    private String track = 'alpha'

    void setTrack(String track) {
        if (!(track in ['alpha', 'beta', 'production'])) {
            throw new IllegalArgumentException("Track has to be one of 'alpha', 'beta', or 'production'.")
        }

        this.track = track
    }

    def getTrack() {
        return track
    }

}
