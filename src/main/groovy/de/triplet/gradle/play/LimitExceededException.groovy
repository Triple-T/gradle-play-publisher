package de.triplet.gradle.play

class LimitExceededException extends IllegalArgumentException {

    LimitExceededException(File file, int limit) {
        super("File '${file.parentFile.parentFile.name} -> ${file.name}' has reached the limit of ${limit} characters.")
    }

}
