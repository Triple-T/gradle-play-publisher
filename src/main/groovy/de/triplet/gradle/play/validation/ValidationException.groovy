package de.triplet.gradle.play.validation

class ValidationException extends IllegalArgumentException {

    private String message

    ValidationException(ValidationError... errors) {
        StringBuilder builder = new StringBuilder();

        for(ValidationError error : errors) {
            builder.append("File \'" + error.getFile() + "\' has reached the limit of " + error.getLimit() + " characters.")
            builder.append("\n")
        }

        message = builder.toString()
    }

    @Override
    String getMessage() {
        return message;
    }

}
