package de.triplet.gradle.play

class LimitExceededException extends IllegalArgumentException {

    private final String message

    LimitExceededException(File file, int limit) {
        message = "File '${file.parentFile.parentFile.name} -> ${file.name}' has reached the limit of ${limit} characters."
    }

    @Override
    String getMessage() {
        return message
    }
}
