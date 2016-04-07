package de.triplet.gradle.play

import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent

class TaskHelper {

    def static readAndTrimFile(File file, int maxCharLength, boolean errorOnSizeLimit) {
        if (file.exists()) {
            def text = file.text
            return normalizeAndTrimText(file, text, maxCharLength, errorOnSizeLimit)
        }

        return ""
    }

    static String normalizeAndTrimText(File file, String text, int maxCharLength, boolean errorOnSizeLimit) {
        def message = text.replaceAll("\\r\\n", "\n")

        if (message.length() > maxCharLength) {
            if (errorOnSizeLimit) {
                throw new LimitExceededException(file, maxCharLength)
            }

            return message.substring(0, maxCharLength)
        }

        return message
    }

    def static List<AbstractInputStreamContent> getImageListAsStream(File listingDir, String graphicPath) {
        File graphicDir = new File(listingDir, graphicPath)
        if (graphicDir.exists()) {
            return graphicDir.listFiles(new ImageFileFilter()).sort().collect { file ->
                new FileContent(AndroidPublisherHelper.MIME_TYPE_IMAGE, file);
            }
        }
        return null
    }

    def static AbstractInputStreamContent getImageAsStream(File listingDir, String graphicPath) {
        File graphicDir = new File(listingDir, graphicPath)
        if (graphicDir.exists()) {
            File[] files = graphicDir.listFiles(new ImageFileFilter())
            if (files.length > 0) {
                File graphicFile = files[0]
                return new FileContent(AndroidPublisherHelper.MIME_TYPE_IMAGE, graphicFile);
            }
        }
        return null
    }
}
