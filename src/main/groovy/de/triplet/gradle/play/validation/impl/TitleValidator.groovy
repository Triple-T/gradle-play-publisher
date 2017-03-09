package de.triplet.gradle.play.validation.impl

import de.triplet.gradle.play.validation.FileLengthValidator

class TitleValidator extends FileLengthValidator {
    @Override
    protected int getMaxLength() {
        return 30
    }
}
