package de.triplet.gradle.play

import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent

class TaskHelper {

    def static readAndTrimFile(File file, int maxCharLength) {
        def message = ""
        if (file.exists()) {
            message = file.text
            if (message.length() > maxCharLength) {
                message.substring(0, maxCharLength)
            }
        }
        return message;
    }

    def static List<AbstractInputStreamContent> getAbstractInputStreamContentList(File listingDir, String graphicPath) {
        File graphicDir = new File(listingDir, graphicPath)
        if (graphicDir.exists()) {
            return graphicDir.listFiles().collect { file ->
                new FileContent(AndroidPublisherHelper.MIME_TYPE_IMAGE, file);
            }
        }
        return null
    }

    def static AbstractInputStreamContent getAbtractInputStreamContentFile(File listingDir, String graphicPath) {
        File graphicDir = new File(listingDir, graphicPath)
        if (graphicDir.exists()) {
            File[] files = graphicDir.listFiles()
            if (files.length > 0) {
                File graphicFile = files[0]
                return new FileContent(AndroidPublisherHelper.MIME_TYPE_IMAGE, graphicFile);
            }
        }
        return null
    }

}
