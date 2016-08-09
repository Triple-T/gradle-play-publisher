package de.triplet.gradle.play

class PlayPublisherPluginExtension {

    String serviceAccountEmail

    File pk12File

    File jsonFile

    boolean uploadImages = false

    boolean errorOnSizeLimit = true

    String untrackFormat = ""

    private String track = 'alpha'

    private String untrack = 'alpha'

    void setTrack(String track) {
        if (!(track in ['alpha', 'beta', 'rollout', 'production'])) {
            throw new IllegalArgumentException("Track has to be one of 'alpha', 'beta', 'rollout' or 'production'.")
        }

        this.track = track
    }

    def getTrack() {
        return track
    }

    void setUntrack(String untrack) {
        if (!(untrack in ['alpha', 'beta', 'rollout'])) {
            throw new IllegalArgumentException("Track has to be one of 'alpha', 'beta' or 'rollout'.")
        }

        this.untrack = untrack
    }

    def getUntrack() {
        return untrack
    }

    Double userFraction = 0.1

}
