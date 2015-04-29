package de.triplet.gradle.play


class LimitExceededException extends IllegalArgumentException {

    private String message = ""

    LimitExceededException(File file, int limit) {
        String place = file.getParentFile().getParentFile().getName() + " -> " + file.getName();
        message = "File \'" + place + "\' has reached the limit of " + limit + " characters."
    }

    @Override
    String getMessage() {
        return message;
    }

    @Override
    String getLocalizedMessage() {
        return message
    }
}
