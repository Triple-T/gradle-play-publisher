package de.triplet.gradle.play.validation

class ValidationError {

    private File file;
    private int limit;

    ValidationError(File file, int limit) {
        this.file = file
        this.limit = limit
    }

    File getFile() {
        return file
    }

    int getLimit() {
        return limit
    }
}
