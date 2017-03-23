package de.triplet.gradle.play

import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent
import org.gradle.api.Project

class TaskHelper {

    private static final MIME_TYPE_IMAGE = 'image/*'

    static readAndTrimFile(Project project, File file, int maxCharLength, boolean errorOnSizeLimit) {
        if (file.exists()) {
            def message = normalize(file.text)

            if (message.length() > maxCharLength) {
                if (errorOnSizeLimit) {
                    def resourcesOutput = project.file(PlayPublisherPlugin.RESOURCES_OUTPUT_PATH)
                    def relativePath = resourcesOutput.toURI().relativize(file.toURI())
                    throw new IllegalArgumentException("File '${relativePath}' has reached the limit of ${maxCharLength} characters")
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
