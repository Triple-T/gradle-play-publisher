package de.triplet.gradle.play

class LimitExceededException extends IllegalArgumentException {

    private String message

    LimitExceededException(File file, int limit) {
        String place = file.parentFile.parentFile.name + " -> " + file.name;
        message = "File \'" + place + "\' has reached the limit of " + limit + " characters."
    }

    @Override
    String getMessage() {
        return message;
    }
}
