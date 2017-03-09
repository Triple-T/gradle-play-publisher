package de.triplet.gradle.play.validation

interface IValidator<T> {

    /**
     * @return true if asset is valid, otherwise false
     */
    boolean validate(T asset)

}