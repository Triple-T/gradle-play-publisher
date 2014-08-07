package de.triplet.gradle.play

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PlayPublishTask extends DefaultTask {

    private PlayPublisherPluginExtension extension;

    @TaskAction
    def publish() {
        println extension.serviceAccountEmail
    }

    void setExtension(PlayPublisherPluginExtension extension) {
        this.extension = extension
    }

}
