package de.triplet.gradle.play.validation.impl

import de.triplet.gradle.play.validation.FileLengthValidator

class ShortDescriptionValidator extends FileLengthValidator {
    @Override
    protected int getMaxLength() {
        return 80
    }
}
