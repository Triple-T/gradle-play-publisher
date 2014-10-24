package de.triplet.gradle.play

class PlayPublisherPluginExtension {

    private String serviceAccountEmail

    private String track = 'alpha'

    private File pk12File

    private boolean uploadImages = false

    void setUploadImages(boolean uploadImages) {
        this.uploadImages = uploadImages
    }

    boolean getUploadImages() {
        return uploadImages
    }

    void setServiceAccountEmail(String email) {
        serviceAccountEmail = email
    }

    def getServiceAccountEmail() {
        return serviceAccountEmail
    }

    void setPk12File(File file) {
        pk12File = file
    }

    def getPk12File() {
        return pk12File
    }

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
