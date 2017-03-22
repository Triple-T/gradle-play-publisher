package de.triplet.gradle.play

import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent

class TaskHelper {

    private static final MIME_TYPE_IMAGE = 'image/*'

    static readAndTrimFile(File file, int maxCharLength, boolean errorOnSizeLimit) {
        if (file.exists()) {
            def message = normalize(file.text)

            if (message.length() > maxCharLength) {
                if (errorOnSizeLimit) {
                    throw new IllegalArgumentException("File '${file.parentFile.parentFile.name} -> ${file.name}' has reached the limit of ${maxCharLength} characters")
                }

                return message.substring(0, maxCharLength)
            }

            return message
        }

        return ''
    }

    static normalize(String text) {
        return text.replaceAll('\\r\\n', '\n').trim()
    }

    static List<AbstractInputStreamContent> getImageListAsStream(File listingDir, String graphicPath) {
        def graphicDir = new File(listingDir, graphicPath)
        if (graphicDir.exists()) {
            return graphicDir.listFiles(new ImageFileFilter()).sort().collect { file ->
                new FileContent(MIME_TYPE_IMAGE, file)
            }
        }
        return null
    }

    static AbstractInputStreamContent getImageAsStream(File listingDir, String graphicPath) {
        def graphicDir = new File(listingDir, graphicPath)
        if (graphicDir.exists()) {
            def files = graphicDir.listFiles(new ImageFileFilter())
            if (files.length > 0) {
                def graphicFile = files[0]
                return new FileContent(MIME_TYPE_IMAGE, graphicFile)
            }
        }
        return null
    }

    static readSingleLine(File file) {
        if (file.exists()) {
            file.withReader { return it.readLine() }
        }
    }
}
