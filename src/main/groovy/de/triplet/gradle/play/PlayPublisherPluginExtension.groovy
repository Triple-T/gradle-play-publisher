package de.triplet.gradle.play

class PlayPublisherPluginExtension {

    private String serviceAccountEmail

    void setServiceAccountEmail(String email) {
        serviceAccountEmail = email
    }

    def getServiceAccountEmail() {
        return serviceAccountEmail
    }
}
