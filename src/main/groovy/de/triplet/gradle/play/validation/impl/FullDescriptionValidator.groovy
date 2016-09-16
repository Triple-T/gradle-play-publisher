package de.triplet.gradle.play.validation.impl

import de.triplet.gradle.play.validation.FileLengthValidator

class FullDescriptionValidator extends FileLengthValidator {
    @Override
    protected int getMaxLength() {
        return 4000
    }
}
