package de.triplet.gradle.play

class PlayPublisherPluginExtension {

    private String serviceAccountEmail

    private File pk12File

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

}
