package de.triplet.gradle.play.validation.impl

import de.triplet.gradle.play.validation.FileLengthValidator

class WhatsnewValidator extends FileLengthValidator {

    @Override
    protected int getMaxLength() {
        return 500
    }
}
