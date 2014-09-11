package de.triplet.gradle.play

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

}
