package de.triplet.gradle.play.validation;

import de.triplet.gradle.play.LimitExceededException;
import de.triplet.gradle.play.TaskHelper;

import java.io.File;

public abstract class FileLengthValidator implements IValidator<File> {

    @Override
    public boolean validate(File asset) {
        try {
            TaskHelper.readAndTrimFile(asset, getMaxLength(), true);
            return true;
        } catch (LimitExceededException e) {
            return false;
        }
    }

    protected abstract int getMaxLength();
}
